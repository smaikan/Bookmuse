package com.kitap.pdf

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteConstraintException
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class AnnotationRepositoryTest {
  private lateinit var context: Context
  private lateinit var repo: BookRepository
  private fun anchor(id: String="a",quote: String="IŞIK İstanbul yüzde 35%",page: Int=41)=AnnotationRepository.Anchor(id,"hash-$id",page,quote,"önce","sonra",0,20,
    "{\"media\":[20,800,620,0],\"crop\":[20,800,620,0],\"rotation\":1}",listOf(floatArrayOf(30f,10f,100f,10f,100f,22f,30f,22f),floatArrayOf(30f,30f,120f,30f,120f,42f,30f,42f)))
  private fun save(quote: String="IŞIK İstanbul yüzde 35%",page: Int=41,id: String=UUID.randomUUID().toString())=JSONObject(repo.annotations.save(anchor(quote=quote,page=page),id,"underline","#c49b23"))
  @Before fun setup() {
    context=RuntimeEnvironment.getApplication();context.deleteDatabase("kitap.db");repo=BookRepository(context)
    for(id in listOf("a","b")) repo.add(BookRepository.Book(id,"content://test/$id","$id.pdf","Kitap $id","hash-$id",3,500,1))
  }
  @After fun close() { repo.close() }
  @Test fun idempotentQuoteAndQuadsSurviveReopen() {
    val id=UUID.randomUUID().toString();save(id=id);save(id=id)
    repo.close();repo=BookRepository(context)
    val rows=JSONArray(repo.annotations.list("a","","","","page",0,40))
    assertEquals(1,rows.length());assertEquals(41,rows.getJSONObject(0).getInt("pageIndex"));assertEquals(2,rows.getJSONObject(0).getInt("lineCount"))
    repo.annotations.ranges("a",41) { c -> assertEquals(2,c.count);c.moveToFirst();assertEquals(30f,c.getFloat(2),0f) }
    assertEquals(0,JSONArray(repo.annotations.list("b","","","","page",0,40)).length())
    assertThrows(IllegalArgumentException::class.java) { save("başka alıntı",id=id) }
    assertThrows(IllegalArgumentException::class.java) { repo.annotations.save(anchor().copy(start=1),id,"underline","#c49b23") }
  }
  @Test fun failedRangeWriteRollsBackBothTablesAndRetrySucceeds() {
    val id=UUID.randomUUID().toString()
    repo.writableDatabase.execSQL("CREATE TRIGGER reject_range BEFORE INSERT ON annotation_ranges BEGIN SELECT RAISE(ABORT,'injected failure'); END")
    assertThrows(SQLiteConstraintException::class.java) { save(id=id) };assertNull(repo.annotations.detail("a",id))
    repo.writableDatabase.execSQL("DROP TRIGGER reject_range");save(id=id)
    assertEquals(1,JSONArray(repo.annotations.list("a","","","","page",0,40)).length())
  }
  @Test fun editDeleteUndoUsesRevisionsAndKeepsRanges() {
    val item=save();val id=item.getString("id")
    val edited=JSONObject(repo.annotations.change("a",id,1,"highlight","#3989b8",false))
    assertEquals(2,edited.getInt("revision"))
    assertThrows(IllegalArgumentException::class.java) { repo.annotations.change("a",id,1,"underline","#c49b23",false) }
    repo.annotations.change("a",id,2,"highlight","#3989b8",true)
    assertEquals("[]",repo.annotations.list("a","","","","page",0,40))
    repo.annotations.change("a",id,3,"highlight","#3989b8",false)
    repo.annotations.ranges("a",41) { assertEquals(2,it.count) }
    assertEquals(MarkerStyle.YELLOW,repo.annotations.detail("a",id)!!.getString("color"))
  }
  @Test fun wrongFingerprintPageOrMalformedGeometryCannotWrite() {
    val id=UUID.randomUUID().toString()
    assertThrows(IllegalArgumentException::class.java) { repo.annotations.save(anchor().copy(fingerprint="new-edition"),id,"underline","#c49b23") }
    assertThrows(IllegalArgumentException::class.java) { save(page=500) }
    assertThrows(IllegalArgumentException::class.java) { repo.annotations.save(anchor().copy(quads=listOf(floatArrayOf(Float.NaN))),id,"underline","#c49b23") }
    assertEquals("[]",repo.annotations.list("a","","","","page",0,40))
  }
  @Test fun thousandMarksStayPaginatedAndFiltersAreLiteralTurkishAndBookScoped() {
    repeat(1000) { index -> save(quote=if(index%2==0) "IŞIK $index %" else "Başka $index",page=index%500) }
    assertEquals(40,JSONArray(repo.annotations.list("a","ışık","highlight","#FFEB3B","page",0,40)).length())
    assertEquals(40,JSONArray(repo.annotations.list("a","%","","","date",40,40)).length())
    assertEquals("[]",repo.annotations.list("a","_","","","page",0,40))
    assertEquals(40,JSONArray(repo.annotations.list("a","","","","page",960,40)).length())
    assertEquals("[]",repo.annotations.list("a","","","","page",1000,40))
    assertEquals("[]",repo.annotations.list("b","","","","page",0,40))
  }
  @Test fun cardsOnlyReturnPreviewAndDetailKeepsWholeQuoteWithoutPdfFile() {
    val full="Kitap alıntısı ".repeat(200);val item=save(quote=full)
    val preview=JSONArray(repo.annotations.list("a","","","","page",0,40)).getJSONObject(0)
    assertEquals(400,preview.getString("quote").length)
    assertEquals(full,repo.annotations.detail("a",item.getString("id"))!!.getString("quote"))
  }
  @Test fun manualRegionsCannotMasqueradeAsText() {
    val region=anchor().copy(quote="Kendi açıklamam",start=-1,end=-1,region=true)
    val id=UUID.randomUUID().toString();repo.annotations.save(region,id,"region","#3989b8")
    assertEquals("manual-region",repo.annotations.detail("a",id)!!.getString("provenance"))
    assertThrows(IllegalArgumentException::class.java) { repo.annotations.change("a",id,1,"underline","#c49b23",false) }
  }
  @Test fun historicSixMigrationPreservesGeometryNotesAndRecoveryAndIsIdempotent() {
    val mark=save();val id=mark.getString("id")
    val draft=JSONObject(repo.notes.open("a","","passage",41,id))
    val db=repo.writableDatabase
    db.execSQL("UPDATE annotations SET kind='underline',color='#3989b8',revision=4 WHERE id=?",arrayOf(id))
    val original=repo.annotations.detail("a",id)!!
    db.version=6;repo.close();repo=BookRepository(context)
    val converted=repo.annotations.detail("a",id)!!
    assertEquals("highlight",converted.getString("kind"));assertEquals(MarkerStyle.YELLOW,converted.getString("color"));assertEquals(5,converted.getInt("revision"))
    for(key in listOf("quote","pageIndex","createdAt","lineCount")) assertEquals(original.get(key),converted.get(key))
    repo.annotations.ranges("a",41) { c -> assertEquals(2,c.count);c.moveToFirst();assertEquals(30f,c.getFloat(2),0f) }
    repo.readableDatabase.rawQuery("SELECT annotation_id FROM note_drafts WHERE id=?",arrayOf(draft.getString("id"))).use { c -> assertTrue(c.moveToFirst());assertEquals(id,c.getString(0)) }
    repo.readableDatabase.rawQuery("SELECT kind,color,revision FROM annotation_style_history WHERE annotation_id=?",arrayOf(id)).use { c -> assertTrue(c.moveToFirst());assertEquals("underline",c.getString(0));assertEquals("#3989b8",c.getString(1));assertEquals(4,c.getInt(2)) }
    MarkerStyle.normalize(repo.writableDatabase);assertEquals(5,repo.annotations.detail("a",id)!!.getInt("revision"))
  }
  @Test fun failedYellowMigrationRollsBackHistoryAndVersionWithoutReset() {
    val id=save().getString("id");val db=repo.writableDatabase
    db.execSQL("UPDATE annotations SET kind='underline',color='#3989b8' WHERE id=?",arrayOf(id))
    db.execSQL("CREATE TRIGGER reject_yellow BEFORE UPDATE ON annotations BEGIN SELECT RAISE(ABORT,'injected migration failure'); END")
    db.version=6;repo.close();repo=BookRepository(context)
    assertThrows(android.database.sqlite.SQLiteException::class.java) { repo.writableDatabase }
    SQLiteDatabase.openDatabase(context.getDatabasePath("kitap.db").path,null,SQLiteDatabase.OPEN_READWRITE).use { raw ->
      assertEquals(6,raw.version)
      raw.rawQuery("SELECT kind,color FROM annotations WHERE id=?",arrayOf(id)).use { c -> assertTrue(c.moveToFirst());assertEquals("underline",c.getString(0)) }
      raw.rawQuery("SELECT count(*) FROM annotation_style_history",null).use { it.moveToFirst();assertEquals(0,it.getInt(0)) }
      raw.execSQL("DROP TRIGGER reject_yellow")
    }
    assertEquals(MarkerStyle.YELLOW,repo.annotations.detail("a",id)!!.getString("color"))
  }
  @Test fun textStylesNormalizeBeforeIdempotentComparisonWhileRegionsKeepTheirStyle() {
    val id=UUID.randomUUID().toString();repo.annotations.save(anchor(),id,"underline","#3989b8")
    val retry=JSONObject(repo.annotations.save(anchor(),id,"highlight",MarkerStyle.YELLOW))
    assertEquals(1,retry.getInt("revision"));assertEquals(MarkerStyle.YELLOW,retry.getString("color"))
    val region=anchor().copy(quote="Tarama",start=-1,end=-1,region=true);val regionId=UUID.randomUUID().toString()
    repo.annotations.save(region,regionId,"region","#3989b8");MarkerStyle.normalize(repo.writableDatabase)
    assertEquals("#3989b8",repo.annotations.detail("a",regionId)!!.getString("color"))
  }
  @Test fun brokenMigrationDoesNotResetOrAdvanceHistoricDatabase() {
    repo.close();context.deleteDatabase("kitap.db")
    val sql=javaClass.classLoader!!.getResourceAsStream("migrations/v3.sql")!!.bufferedReader().use { it.readText() }
    SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("kitap.db"),null).use { db ->
      sql.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) };db.execSQL("CREATE TABLE annotations(existing TEXT)");db.version=3
    }
    repo=BookRepository(context)
    assertThrows(android.database.sqlite.SQLiteException::class.java) { repo.writableDatabase }
    SQLiteDatabase.openDatabase(context.getDatabasePath("kitap.db").path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
      assertEquals(3,db.version);db.rawQuery("SELECT page_index FROM reading_positions WHERE document_id='old-book'",null).use { it.moveToFirst();assertEquals(41,it.getInt(0)) }
    }
  }
  @Test fun historicVersionThreeMigrationKeepsPositionSettingsBookmarksAndMode() {
    repo.close();context.deleteDatabase("kitap.db")
    val sql=javaClass.classLoader!!.getResourceAsStream("migrations/v3.sql")!!.bufferedReader().use { it.readText() }
    SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("kitap.db"),null).use { db -> sql.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) };db.version=3 }
    repo=BookRepository(context)
    assertEquals(41,repo.find("old-book")!!.pageIndex);assertEquals(3,repo.options("old-book").theme)
    assertTrue(repo.options("old-book").leftHand);assertTrue(repo.options("old-book").doublePage)
    assertEquals("[41]",repo.bookmarks("old-book",0,40));assertEquals("grid",repo.libraryMode())
    assertEquals("[]",repo.annotations.list("old-book","","","","page",0,40));assertEquals(7,repo.readableDatabase.version)
  }
}
