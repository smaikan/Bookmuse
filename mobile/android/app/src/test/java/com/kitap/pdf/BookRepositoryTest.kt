package com.kitap.pdf

import android.content.Context
import android.app.Application
import org.robolectric.RuntimeEnvironment
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Real repository and SQLite in a simulated Android runtime, not a mock SQL reimplementation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class BookRepositoryTest {
  private lateinit var context: Context
  private lateinit var repository: BookRepository
  private fun book(id: String) = BookRepository.Book(id, "content://provider/$id", "$id.pdf", "Kitap $id", "hash-$id", 3L, 500, 1L)
  @Before fun prepare() {
    context = RuntimeEnvironment.getApplication()
    context.deleteDatabase("kitap.db")
    File(context.filesDir, "books").deleteRecursively()
    repository = BookRepository(context)
    repository.recover()
  }
  @After fun clean() { repository.close() }

  @Test fun reopeningKeepsIndependentBookPositions() {
    repository.add(book("a")); repository.add(book("b"))
    repository.savePage("a", 41); repository.savePage("b", 499)
    repository.close()
    repository = BookRepository(context)
    assertEquals(41, repository.find("a")!!.pageIndex)
    assertEquals(499, repository.find("b")!!.pageIndex)
    val first = org.json.JSONArray(repository.list(0, 1))
    val second = org.json.JSONArray(repository.list(1, 1))
    assertEquals(1, first.length()); assertEquals(1, second.length())
    assertNotEquals(first.getJSONObject(0).getString("id"), second.getJSONObject(0).getString("id"))
  }
  @Test fun duplicateUriCannotLeaveAPartialRow() {
    repository.add(book("a"))
    assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
      repository.add(book("b").copy(uri = book("a").uri))
    }
    assertNull(repository.find("b"))
    assertEquals(1, org.json.JSONArray(repository.list(0, 40)).length())
  }
  @Test fun aPositionWriteFailureRollsBackTheDocumentRowToo() {
    repository.writableDatabase.execSQL("""CREATE TRIGGER reject_position BEFORE INSERT ON reading_positions
      BEGIN SELECT RAISE(ABORT,'injected storage failure'); END""")
    assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) { repository.add(book("a")) }
    assertNull(repository.find("a"))
    assertEquals(0, org.json.JSONArray(repository.list(0, 40)).length())
  }
  @Test fun removalCleansOwnedCopyAndPositionButPreservesExternalOriginal() {
    val external = File(context.cacheDir, "external-original.pdf").apply { writeText("abc") }
    val a = book("a")
    repository.add(a); repository.file(a).writeText("abc"); repository.savePage("a", 10)
    repository.remove("a")
    assertNull(repository.find("a"))
    assertFalse(repository.file(a).exists())
    assertTrue(external.isFile)
    repository.readableDatabase.rawQuery("SELECT COUNT(*) FROM reading_positions", null).use {
      it.moveToFirst(); assertEquals(0, it.getInt(0))
    }
  }
  @Test fun startupRecoversInterruptedImportsWithoutDeletingReadyBooks() {
    val a = book("a")
    repository.add(a); repository.file(a).writeText("abc")
    File(repository.directory, "unfinished.pdf.part").writeText("partial")
    File(repository.directory, "orphan.pdf").writeText("renamed before commit")
    repository.close(); repository = BookRepository(context); repository.recover()
    assertEquals(listOf("a.pdf"), repository.directory.listFiles()!!.map { it.name })
    assertEquals("a", repository.find("a")!!.id)
  }
  @Test fun relinkKeepsBookIdentityAndPositionAndInvalidPageCannotOverwriteIt() {
    repository.add(book("a")); repository.savePage("a", 41)
    repository.relink("a", "content://other/same-book", "replacement.pdf")
    assertEquals(41, repository.find("a")!!.pageIndex)
    assertEquals("a", repository.byUri("content://other/same-book")!!.id)
    assertThrows(IllegalArgumentException::class.java) { repository.savePage("a", 500) }
    assertEquals(41, repository.find("a")!!.pageIndex)
  }
  @Test fun versionOneMigrationKeepsTheOldPositionAndAddsDurablePreferences() {
    repository.close(); context.deleteDatabase("kitap.db")
    val fixture = checkNotNull(javaClass.classLoader!!.getResourceAsStream("migrations/v1.sql")).bufferedReader().use { it.readText() }
    android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("kitap.db"), null).use { db ->
      fixture.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) }; db.version = 1
    }
    repository = BookRepository(context)
    assertEquals(41, repository.find("old-book")!!.pageIndex)
    repository.saveOptions("old-book", ReaderOptions(theme=3, left=.1, brightness=.4))
    repository.close(); repository = BookRepository(context)
    assertEquals(3, repository.options("old-book").theme)
    assertEquals(.1, repository.options("old-book").left, 0.0)
    assertEquals(41, repository.find("old-book")!!.pageIndex)
  }
  @Test fun bookmarksAreScopedIdempotentAndSurviveReopen() {
    repository.add(book("a"));repository.add(book("b"))
    repository.setBookmark("a",41,true);repository.setBookmark("a",41,true)
    repository.close();repository=BookRepository(context)
    assertEquals("[41]",repository.bookmarks("a",0,40))
    assertFalse(repository.isBookmarked("b",41))
    repository.setBookmark("a",41,false);assertEquals("[]",repository.bookmarks("a",0,40))
  }
  @Test fun titleSearchIsTurkishAwareAndPercentIsLiteral() {
    repository.add(book("a").copy(title="IŞIK yüzde 35%"));repository.add(book("b").copy(title="Başka kitap"))
    assertEquals(1,org.json.JSONArray(repository.list(0,40,"ışık","title")).length())
    assertEquals(1,org.json.JSONArray(repository.list(0,40,"%","recent")).length())
    assertEquals(0,org.json.JSONArray(repository.list(0,40,"_","recent")).length())
  }
  @Test fun renamedTitleKeepsPositionAndOnlyReadBooksAppearInContinue() {
    repository.add(book("a"));repository.add(book("b"))
    assertEquals("null",repository.mostRecent())
    repository.savePage("a",41);repository.rename("a","Yeni ad")
    assertEquals(41,repository.find("a")!!.pageIndex)
    assertEquals("Yeni ad",org.json.JSONObject(repository.mostRecent()).getString("title"))
  }
  @Test fun libraryModeAndThumbnailLruMetadataPersistIndependently() {
    repository.updateLibraryMode("grid");repository.touchThumbnail("old",10);repository.touchThumbnail("new",20)
    repository.close();repository=BookRepository(context)
    assertEquals("grid",repository.libraryMode());assertEquals(30L,repository.thumbnailBytes())
    repository.forgetThumbnail("old");assertEquals(20L,repository.thumbnailBytes())
  }
  @Test fun favoriteFilterCombinesSearchAndOrderAndSurvivesReopen() {
    repository.add(book("a").copy(title="IŞIK bir"));repository.add(book("b").copy(title="IŞIK iki"));repository.add(book("c"))
    repository.setFavorite("b",true);repository.setFavorite("c",true)
    assertEquals("b",org.json.JSONArray(repository.list(0,40,"ışık","title",true)).getJSONObject(0).getString("id"))
    assertEquals(1,org.json.JSONArray(repository.list(0,1,"","recent",true)).length())
    repository.close();repository=BookRepository(context)
    assertTrue(repository.find("b")!!.favorite);assertFalse(repository.find("a")!!.favorite)
    repository.setFavorite("b",false);assertEquals(0,org.json.JSONArray(repository.list(0,40,"ışık","recent",true)).length())
  }
  @Test fun libraryPreferencesAreAtomicAndDoNotChangeReadingPosition() {
    repository.add(book("a"));repository.savePage("a",44);repository.saveLibraryPreferences("title",true)
    repository.close();repository=BookRepository(context)
    val prefs=org.json.JSONObject(repository.libraryPreferences());assertEquals("title",prefs.getString("sort"));assertTrue(prefs.getBoolean("favoritesOnly"));assertEquals(44,repository.find("a")!!.pageIndex)
    repository.writableDatabase.execSQL("CREATE TRIGGER reject_filter BEFORE INSERT ON app_preferences WHEN NEW.key='libraryFilter' BEGIN SELECT RAISE(ABORT,'test'); END")
    assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) { repository.saveLibraryPreferences("recent",false) }
    assertEquals("title",org.json.JSONObject(repository.libraryPreferences()).getString("sort"))
  }
  @Test fun legacyHelperPreferenceIsIgnoredWithoutReindexingThemesOrLosingPosition() {
    repository.add(book("a")); repository.savePage("a",44); repository.setFavorite("a",true)
    repository.saveReaderDefaults("""{"theme":3,"helperControls":true,"doublePage":true}""")
    repository.saveOptions("a",ReaderOptions.parse("""{"theme":1,"helperControls":true}"""))
    repository.close(); repository=BookRepository(context)
    assertEquals(3,repository.readerDefaults().theme)
    assertTrue(repository.readerDefaults().doublePage)
    assertFalse(repository.readerDefaults().json().has("helperControls"))
    assertEquals(1,repository.options("a").theme)
    assertEquals(44,repository.find("a")!!.pageIndex)
    assertTrue(org.json.JSONArray(repository.list(0,40,"","recent",true)).length()==1)
  }
  @Test fun sharedReaderDefaultsIgnoreHistoricBookCropAndNormalizeGeometry() {
    repository.add(book("a"));repository.savePage("a",44);repository.saveOptions("a",ReaderOptions(theme=3,left=.2,fitWidth=true))
    assertEquals(0,repository.readerDefaults().theme);assertEquals(0.0,repository.readerDefaults().left,0.0)
    repository.saveReaderDefaults(ReaderOptions(theme=2,left=.2,right=.1,fitWidth=true,fullscreen=false).json().toString())
    repository.close();repository=BookRepository(context)
    val common=repository.readerDefaults();assertEquals(2,common.theme);assertEquals(0.0,common.left,0.0);assertFalse(common.fitWidth);assertTrue(common.fullscreen);assertFalse(common.counterAlways)
    assertEquals(.2,repository.options("a").left,0.0);assertEquals(44,repository.find("a")!!.pageIndex)
  }
}
