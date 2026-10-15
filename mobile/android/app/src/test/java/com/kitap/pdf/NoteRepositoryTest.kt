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
class NoteRepositoryTest {
  private lateinit var context: Context
  private lateinit var repo: BookRepository
  private lateinit var mark: String
  private fun anchor()=AnnotationRepository.Anchor("a","hash-a",41,"Özgün pasaj alıntısı","önce","sonra",0,10,"{\"media\":[0,800,600,0],\"crop\":[0,800,600,0],\"rotation\":0}",listOf(floatArrayOf(20f,30f,100f,30f,100f,42f,20f,42f)))
  private fun open(scope: String="book",id: String="")=JSONObject(repo.notes.open("a",id,scope,if(scope=="book") -1 else 41,if(scope=="passage") mark else ""))
  private fun write(d: JSONObject,v: Int,body: String,title: String="Başlık")=JSONObject(repo.notes.write("a",d.getString("id"),d.getString("editorToken"),v,title,body))
  private fun save(d: JSONObject,v: Int,body: String,title: String="Başlık")=JSONObject(repo.notes.save("a",d.getString("id"),d.getString("editorToken"),v,title,body))
  private fun list(query: String="",scope: String="",offset: Int=0)=JSONArray(repo.notes.list("a",query,scope,"date",offset,40))
  @Before fun setup() {
    context=RuntimeEnvironment.getApplication();context.deleteDatabase("kitap.db");repo=BookRepository(context)
    for(id in listOf("a","b"))repo.add(BookRepository.Book(id,"content://test/$id","$id.pdf","Kitap $id","hash-$id",3,500,1))
    mark=UUID.randomUUID().toString();repo.annotations.save(anchor(),mark,"underline","#c49b23")
  }
  @After fun close() { repo.close() }
  @Test fun bookPagePassageBindingsKeepQuoteSeparateFromBody() {
    for(scope in listOf("book","page","passage"))save(open(scope),1,"Benim düşüncem $scope")
    assertEquals(3,list().length());assertEquals(-1,list(scope="book").getJSONObject(0).getInt("pageIndex"))
    val passage=list(scope="passage").getJSONObject(0);assertEquals(mark,passage.getString("annotationId"));assertEquals("Özgün pasaj alıntısı",passage.getString("quote"));assertEquals("Benim düşüncem passage",passage.getString("body"))
    assertEquals("[]",repo.notes.list("b","","","date",0,40))
    assertThrows(IllegalArgumentException::class.java) { repo.notes.open("b",passage.getString("id"),"book",-1,"") }
  }
  @Test fun reopeningFromEitherTabUsesSamePassageNoteAndRotatesEditorLease() {
    val first=open("passage");write(first,1,"Türkçe taslak 😀")
    val second=open("passage");assertEquals(first.getString("id"),second.getString("id"));assertTrue(second.getBoolean("recovered"))
    assertThrows(IllegalArgumentException::class.java) { write(first,2,"Eski editör") }
    save(second,2,"Son not");val third=open("passage");assertEquals(first.getString("id"),third.getString("id"));assertEquals("Son not",third.getString("body"))
    assertEquals(1,list().length())
  }
  @Test fun revisionTwoBeforeOneAndReopenKeepsLatestCommittedDraft() {
    val d=open();write(d,2,"İstanbul son taslak 😀")
    assertThrows(IllegalArgumentException::class.java) { write(d,1,"Eski metin") }
    repo.close();repo=BookRepository(context);val recovered=open(id=d.getString("id"))
    assertEquals(2,recovered.getInt("contentRevision"));assertEquals("İstanbul son taslak 😀",recovered.getString("body"));assertTrue(recovered.getBoolean("recovered"))
    assertThrows(IllegalArgumentException::class.java) { write(d,3,"Eski token") }
    assertEquals(1,list(scope="draft").length())
  }
  @Test fun finalSaveIsAtomicRetryableAndClosesTheDraftLease() {
    val d=open();write(d,1,"Taslak")
    repo.writableDatabase.execSQL("CREATE TRIGGER reject_note BEFORE INSERT ON notes BEGIN SELECT RAISE(ABORT,'injected failure'); END")
    assertThrows(SQLiteConstraintException::class.java) { save(d,2,"Son not") }
    assertEquals("Taslak",list().getJSONObject(0).getString("body"))
    repo.writableDatabase.execSQL("DROP TRIGGER reject_note")
    val saved=save(d,2,"Son not");val retry=save(d,2,"Son not")
    assertEquals(saved.getInt("revision"),retry.getInt("revision"));assertEquals(1,list().length());assertFalse(list().getJSONObject(0).getBoolean("hasDraft"))
    assertThrows(IllegalStateException::class.java) { write(d,3,"Geç kalan autosave") }
  }
  @Test fun explicitCloseFlushesAndEmptyUnchangedEditorDoesNotClutterRecovery() {
    val empty=open();repo.notes.end("a",empty.getString("id"),empty.getString("editorToken"),0,"","");assertEquals(0,list().length())
    val d=open("page");repo.notes.end("a",d.getString("id"),d.getString("editorToken"),1,"","Çıkışta flush")
    val recovered=open(id=d.getString("id"));assertEquals("Çıkışta flush",recovered.getString("body"));assertTrue(recovered.getBoolean("recovered"))
  }
  @Test fun preservingAnnotationNotesAndDeletingBothHaveDifferentDurableUndo() {
    val d=open("passage");save(d,1,"Pasaj notu")
    repo.annotations.deleteWithNotes("a",mark,1,false);assertEquals(1,list().length());assertTrue(list().getJSONObject(0).getBoolean("annotationDeleted"))
    repo.annotations.change("a",mark,2,"underline","#c49b23",false)
    val editor=open("passage");write(editor,2,"Yeni taslak")
    repo.annotations.deleteWithNotes("a",mark,3,true);assertEquals(0,list().length())
    assertThrows(IllegalArgumentException::class.java) { write(editor,3,"Silinmiş editör") }
    repo.close();repo=BookRepository(context)
    repo.annotations.change("a",mark,4,"underline","#c49b23",false)
    val restored=open("passage");assertEquals("Yeni taslak",restored.getString("body"));assertEquals(1,list().length())
  }
  @Test fun groupUndoDoesNotResurrectPreviouslyDeletedNotes() {
    val old=open("passage");val oldSaved=save(old,1,"Eski silinmiş not")
    repo.notes.changeDeleted("a",oldSaved.getString("id"),1,false,true)
    val newer=open("passage");save(newer,1,"Yeni aktif not")
    repo.annotations.deleteWithNotes("a",mark,1,true);repo.annotations.change("a",mark,2,"underline","#c49b23",false)
    assertEquals(1,list().length());assertEquals(newer.getString("id"),list().getJSONObject(0).getString("id"))
  }
  @Test fun noteDeleteUndoPreservesDirtyDraftAndUnsavedDraftCanAlsoBeRestored() {
    val d=open();save(d,1,"Son not");val editor=open(id=d.getString("id"));write(editor,1,"Düzenlenmiş taslak")
    repo.notes.changeDeleted("a",d.getString("id"),1,false,true);assertEquals(0,list().length())
    repo.notes.changeDeleted("a",d.getString("id"),2,false,false);assertEquals("Düzenlenmiş taslak",open(id=d.getString("id")).getString("body"))
    val draft=open();write(draft,1,"Yalnız taslak");repo.notes.changeDeleted("a",draft.getString("id"),1,true,true)
    repo.notes.changeDeleted("a",draft.getString("id"),2,true,false);assertEquals("Yalnız taslak",open(id=draft.getString("id")).getString("body"))
  }
  @Test fun passageCreationRollsBackAnnotationIfDraftInsertFails() {
    val id=UUID.randomUUID().toString()
    repo.writableDatabase.execSQL("CREATE TRIGGER reject_draft BEFORE INSERT ON note_drafts BEGIN SELECT RAISE(ABORT,'injected draft failure'); END")
    assertThrows(SQLiteConstraintException::class.java) { repo.createPassageNote(anchor(),id,"#c49b23") }
    assertNull(repo.annotations.detail("a",id));repo.writableDatabase.execSQL("DROP TRIGGER reject_draft")
    val first=JSONObject(repo.createPassageNote(anchor(),id,"#c49b23"));val second=JSONObject(repo.createPassageNote(anchor(),id,"#c49b23"))
    assertEquals(first.getString("id"),second.getString("id"));assertNotNull(repo.annotations.detail("a",id))
  }
  @Test fun thousandNotesLongBodiesAndLiteralSearchReturnOnlyPaginatedPreviews() {
    repeat(1000) { index -> save(open(),1,if(index==0) "a".repeat(50000) else "Not $index yüzde %",if(index%2==0) "IŞIK $index" else "Başka $index") }
    assertEquals(40,list("ışık").length());assertEquals(40,list("%",offset=40).length());assertEquals(0,list("_").length())
    assertEquals(40,list(offset=960).length());assertEquals(0,list(offset=1000).length())
    assertTrue((0 until list().length()).all { list().getJSONObject(it).getString("body").length<=400 })
  }
  @Test fun removingBookCascadesOwnedNotesButCannotTouchOtherBooks() {
    save(open("passage"),1,"a kitabı");val b=JSONObject(repo.notes.open("b","","book",-1,""))
    repo.notes.save("b",b.getString("id"),b.getString("editorToken"),1,"","b kitabı")
    repo.remove("a");assertEquals("[]",repo.notes.list("a","","","date",0,40));assertEquals(1,JSONArray(repo.notes.list("b","","","date",0,40)).length())
  }
  private fun historicV4(broken: Boolean=false) {
    repo.close();context.deleteDatabase("kitap.db")
    SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("kitap.db"),null).use { db ->
      for(name in listOf("migrations/v3.sql","migrations/v4-annotations.sql")) {
        val fixture=javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().use { it.readText() }
        fixture.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) }
      }
      if(broken) db.execSQL("CREATE TABLE notes(existing TEXT)")
      db.version=4
    }
    repo=BookRepository(context)
  }
  @Test fun versionFourMigrationKeepsMarksPositionsPreferencesAndBookmarks() {
    historicV4();assertEquals(7,repo.readableDatabase.version);assertEquals(41,repo.find("old-book")!!.pageIndex)
    assertEquals(3,repo.options("old-book").theme);assertEquals("[41]",repo.bookmarks("old-book",0,40))
    val old="60722458-59b7-4bbd-9547-1f3d24cf7ebc";assertEquals("Eski pasaj",repo.annotations.detail("old-book",old)!!.getString("quote"))
    repo.annotations.ranges("old-book",41) { it.moveToFirst();assertEquals(20f,it.getFloat(2),0f) }
    val d=JSONObject(repo.notes.open("old-book","","passage",41,old));assertEquals("Eski pasaj",d.getString("quote"))
  }
  @Test fun versionFiveMigrationFailureRollsBackWithoutResettingOldRecords() {
    historicV4(true);assertThrows(android.database.sqlite.SQLiteException::class.java) { repo.writableDatabase }
    SQLiteDatabase.openDatabase(context.getDatabasePath("kitap.db").path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
      assertEquals(4,db.version);db.rawQuery("SELECT quote FROM annotations",null).use { it.moveToFirst();assertEquals("Eski pasaj",it.getString(0)) }
      db.rawQuery("PRAGMA table_info(annotations)",null).use { c -> while(c.moveToNext()) assertNotEquals("note_delete_group",c.getString(1)) }
    }
  }
  @Test fun openingUnchangedSavedNoteDoesNotChangeItsOrderingTimestamp() {
    val d=open();val saved=save(d,1,"Kalıcı not");open(id=d.getString("id"))
    assertEquals(saved.getLong("updatedAt"),list().getJSONObject(0).getLong("updatedAt"))
  }
}
