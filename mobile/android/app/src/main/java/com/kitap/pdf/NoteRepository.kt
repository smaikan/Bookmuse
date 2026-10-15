package com.kitap.pdf

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Serial worker only. Editor lease and content revision are independent from final note revision. */
class NoteRepository(private val books: BookRepository) {
  fun open(book: String,id: String,scope: String,page: Int,annotation: String): String = transaction {
    val owner=books.find(book) ?: error("Kitap bulunamadı")
    require(scope in setOf("book","page","passage"))
    if(scope!="book") PageIndex.requireValid(page,owner.pageCount)
    var noteId=id
    if(noteId.isEmpty() && scope=="passage") {
      require(annotation.isNotEmpty())
      noteId=db.rawQuery("SELECT id FROM notes WHERE document_id=? AND annotation_id=? AND deleted=0 UNION ALL SELECT id FROM note_drafts WHERE document_id=? AND annotation_id=? AND deleted=0 AND NOT EXISTS(SELECT 1 FROM notes WHERE notes.id=note_drafts.id) LIMIT 1",arrayOf(book,annotation,book,annotation)).use { if(it.moveToFirst()) it.getString(0) else "" }
    }
    if(noteId.isEmpty()) noteId=UUID.randomUUID().toString()
    require(UUID.fromString(noteId).toString()==noteId)
    val saved=savedNote(book,noteId)
    val existing=draft(book,noteId)
    if(id.isNotEmpty()) require(saved!=null || existing!=null) { "Not bu kitaba ait değil veya bulunamadı" }
    if(saved!=null) require(!saved.getBoolean("deleted")) { "Not silinmiş; önce geri alın" }
    if(existing!=null) require(!existing.getBoolean("deleted")) { "Taslak silinmiş; önce geri alın" }
    val binding=existing ?: saved
    val actualScope=binding?.getString("scope") ?: scope
    val actualPage=binding?.getInt("pageIndex") ?: if(scope=="book") -1 else page
    val actualAnnotation=binding?.getString("annotationId") ?: if(scope=="passage") annotation else ""
    var quote=binding?.getString("quote") ?: ""
    if(binding==null && scope=="passage") {
      val mark=books.annotations.detail(book,annotation) ?: error("Pasaj işareti bulunamadı")
      require(!mark.getBoolean("deleted") && mark.getInt("pageIndex")==page)
      quote=mark.getString("quote")
    }
    val title=existing?.getString("title") ?: saved?.getString("title") ?: ""
    val body=existing?.getString("body") ?: saved?.getString("body") ?: ""
    val token=UUID.randomUUID().toString()
    val version=existing?.getInt("contentRevision") ?: 0
    val dirty=existing?.optBoolean("dirty",false) ?: false
    db.insertWithOnConflict("note_drafts",null,ContentValues().apply {
      put("id",noteId);put("document_id",book);put("scope",actualScope);put("page_index",actualPage)
      put("annotation_id",actualAnnotation.ifEmpty { null });put("quote",quote)
      put("title",title);put("body",body);put("search_text",search(title,body))
      put("editor_token",token);put("content_revision",version);put("base_revision",saved?.getInt("revision") ?: 0)
      put("dirty",if(dirty) 1 else 0);put("deleted",0);put("updated_at",existing?.getLong("updatedAt") ?: saved?.getLong("updatedAt") ?: System.currentTimeMillis())
    },SQLiteDatabase.CONFLICT_REPLACE).also { check(it!=-1L) }
    checkNotNull(draft(book,noteId)).put("recovered",dirty).toString()
  }
  fun write(book: String,id: String,token: String,version: Int,title: String,body: String): String = transaction {
    val value=writeInside(book,id,token,version,title,body)
    JSONObject().put("contentRevision",value.getInt("contentRevision")).put("dirty",value.getBoolean("dirty")).put("updatedAt",value.getLong("updatedAt")).toString()
  }
  private fun writeInside(book: String,id: String,token: String,version: Int,title: String,body: String): JSONObject {
    validate(title,body);require(version>=0)
    val old=draft(book,id) ?: error("Editör kapanmış; son kayıt korundu")
    require(!old.getBoolean("deleted") && old.getString("editorToken")==token) { "Bu editör eski; yeni taslak korunuyor" }
    val saved=savedNote(book,id)
    require((saved?.getInt("revision") ?: 0)==old.getInt("baseRevision") && saved?.optBoolean("deleted",false)!=true) { "Not başka işlemde değişti; metniniz editörde korunuyor" }
    val previous=old.getInt("contentRevision")
    require(version>=previous) { "Eski taslak yazısı reddedildi" }
    if(version==previous) {
      require(title==old.getString("title") && body==old.getString("body")) { "Aynı revision farklı içerik taşıyor" }
      return old
    }
    val dirty=title!=(saved?.getString("title") ?: "") || body!=(saved?.getString("body") ?: "")
    check(db.update("note_drafts",ContentValues().apply {
      put("title",title);put("body",body);put("search_text",search(title,body));put("content_revision",version)
      put("dirty",if(dirty) 1 else 0);put("updated_at",System.currentTimeMillis())
    },"document_id=? AND id=? AND editor_token=?",arrayOf(book,id,token))==1)
    return checkNotNull(draft(book,id))
  }
  fun save(book: String,id: String,token: String,version: Int,title: String,body: String): String = transaction {
    validate(title,body);require(title.isNotBlank() || body.isNotBlank()) { "Başlık veya not metni yazın" }
    val saved=savedNote(book,id)
    if(saved!=null && saved.optString("lastEditor")==token && saved.optInt("lastDraftRevision",-1)==version) {
      require(saved.getString("title")==title && saved.getString("body")==body && !saved.getBoolean("deleted"))
      return@transaction saved.toString()
    }
    val latest=writeInside(book,id,token,version,title,body)
    val nextRevision=latest.getInt("baseRevision")+1
    require(nextRevision>0) { "Not sürüm sınırı aşıldı; kayıt korunuyor" }
    val now=System.currentTimeMillis()
    val values=ContentValues().apply {
      put("id",id);put("document_id",book);put("scope",latest.getString("scope"));put("page_index",latest.getInt("pageIndex"))
      put("annotation_id",latest.getString("annotationId").ifEmpty { null });put("quote",latest.getString("quote"))
      put("title",title);put("body",body);put("search_text",search(title,body));put("revision",nextRevision)
      put("created_at",saved?.getLong("createdAt") ?: now);put("updated_at",now);put("deleted",0)
      put("last_editor",token);put("last_draft_revision",version)
    }
    if(saved==null) db.insertOrThrow("notes",null,values)
    else check(db.update("notes",values,"document_id=? AND id=? AND revision=?",arrayOf(book,id,latest.getInt("baseRevision").toString()))==1)
    db.delete("note_drafts","document_id=? AND id=? AND editor_token=?",arrayOf(book,id,token))
    checkNotNull(savedNote(book,id)).toString()
  }
  fun end(book: String,id: String,token: String,version: Int,title: String,body: String) = transaction {
    val old=draft(book,id) ?: return@transaction Unit
    require(old.getString("editorToken")==token) { "Yeni editör korunuyor" }
    val latest=writeInside(book,id,token,version,title,body)
    if(!latest.getBoolean("dirty")) db.delete("note_drafts","document_id=? AND id=?",arrayOf(book,id))
    else db.update("note_drafts",ContentValues().apply { put("editor_token","") },"document_id=? AND id=?",arrayOf(book,id))
  }
  fun list(book: String,query: String,scope: String,sort: String,offset: Int,limit: Int): String {
    require(query.length<=200 && scope in setOf("","book","page","passage","draft") && sort in setOf("date","page") && offset>=0 && limit in 1..100)
    val pattern="%${BookRepository.normalizeTitle(query).replace("!","!!").replace("%","!%").replace("_","!_")}%"
    val order=if(sort=="date") "updated_at DESC,id" else "page_index,updated_at DESC,id"
    val result=JSONArray()
    db.rawQuery("""SELECT id,scope,page_index,annotation_id,quote,title,body,revision,updated_at,is_draft,draft_only,
      COALESCE((SELECT deleted FROM annotations WHERE annotations.id=v.annotation_id),0)
      FROM ($LIST) v WHERE document_id=? AND search_text LIKE ? ESCAPE '!'
      AND (?='' OR scope=? OR (?='draft' AND is_draft=1)) ORDER BY $order LIMIT ? OFFSET ?""",
      arrayOf(book,pattern,scope,scope,scope,limit.toString(),offset.toString())).use { c -> while(c.moveToNext()) result.put(JSONObject()
        .put("id",c.getString(0)).put("scope",c.getString(1)).put("pageIndex",c.getInt(2)).put("annotationId",c.getString(3) ?: "")
        .put("quote",c.getString(4)).put("title",c.getString(5)).put("body",c.getString(6)).put("revision",c.getInt(7))
        .put("updatedAt",c.getLong(8)).put("hasDraft",c.getInt(9)==1).put("draftOnly",c.getInt(10)==1).put("annotationDeleted",c.getInt(11)==1)) }
    return result.toString()
  }
  fun relatedCount(book: String,annotation: String): Int = db.rawQuery("SELECT COUNT(*) FROM ($LIST) WHERE document_id=? AND annotation_id=?",arrayOf(book,annotation))
    .use { it.moveToFirst();it.getInt(0) }
  fun changeDeleted(book: String,id: String,revision: Int,draftOnly: Boolean,deleted: Boolean): String = transaction {
    val table=if(draftOnly) "note_drafts" else "notes"
    val old=(if(draftOnly) draft(book,id) else savedNote(book,id)) ?: error("Not bulunamadı")
    val field=if(draftOnly) "content_revision" else "revision"
    val oldRevision=old.getInt(if(draftOnly) "contentRevision" else "revision")
    require(revision==oldRevision && revision<Int.MAX_VALUE) { "Not değişti; listeyi yenileyin" }
    val values=ContentValues().apply { put("deleted",if(deleted) 1 else 0);put(field,revision+1);put("updated_at",System.currentTimeMillis());putNull("deletion_group") }
    if(draftOnly) values.put("editor_token","")
    check(db.update(table,values,"document_id=? AND id=? AND $field=?",arrayOf(book,id,revision.toString()))==1)
    if(!draftOnly) db.update("note_drafts",ContentValues().apply { put("editor_token","");put("base_revision",revision+1) },"document_id=? AND id=?",arrayOf(book,id))
    JSONObject().put("id",id).put("revision",revision+1).put("draftOnly",draftOnly).put("deleted",deleted).toString()
  }
  fun deleteRelated(book: String,annotation: String,group: String) {
    db.execSQL("UPDATE notes SET deleted=1,revision=revision+1,deletion_group=?,updated_at=? WHERE document_id=? AND annotation_id=? AND deleted=0",arrayOf<Any>(group,System.currentTimeMillis(),book,annotation))
    db.execSQL("UPDATE note_drafts SET deleted=1,editor_token='',content_revision=content_revision+1,deletion_group=? WHERE document_id=? AND annotation_id=? AND deleted=0 AND (NOT EXISTS(SELECT 1 FROM notes WHERE notes.id=note_drafts.id) OR EXISTS(SELECT 1 FROM notes WHERE notes.id=note_drafts.id AND notes.deletion_group=?))",arrayOf(group,book,annotation,group))
  }
  fun restoreRelated(book: String,annotation: String) {
    val group=db.rawQuery("SELECT note_delete_group FROM annotations WHERE document_id=? AND id=?",arrayOf(book,annotation)).use { if(it.moveToFirst()) it.getString(0) else null } ?: return
    db.execSQL("UPDATE notes SET deleted=0,revision=revision+1,deletion_group=NULL,updated_at=? WHERE document_id=? AND deletion_group=?",arrayOf<Any>(System.currentTimeMillis(),book,group))
    db.execSQL("UPDATE note_drafts SET deleted=0,editor_token='',content_revision=content_revision+1,deletion_group=NULL,base_revision=COALESCE((SELECT revision FROM notes WHERE notes.id=note_drafts.id),0) WHERE document_id=? AND deletion_group=?",arrayOf(book,group))
    db.update("annotations",ContentValues().apply { putNull("note_delete_group") },"document_id=? AND id=?",arrayOf(book,annotation))
  }
  private val db get()=books.writableDatabase
  private fun savedNote(book: String,id: String): JSONObject? = read("notes",book,id,false)
  private fun draft(book: String,id: String): JSONObject? = read("note_drafts",book,id,true)
  private fun read(table: String,book: String,id: String,draft: Boolean): JSONObject? = db.rawQuery("SELECT * FROM $table WHERE document_id=? AND id=?",arrayOf(book,id)).use { c ->
    if(!c.moveToFirst()) null else {
      fun text(key: String)=c.getString(c.getColumnIndexOrThrow(key)) ?: ""
      fun number(key: String)=c.getLong(c.getColumnIndexOrThrow(key))
      val value=JSONObject().put("id",text("id")).put("scope",text("scope")).put("pageIndex",number("page_index"))
        .put("annotationId",text("annotation_id")).put("quote",text("quote")).put("title",text("title")).put("body",text("body"))
        .put("updatedAt",number("updated_at")).put("deleted",number("deleted")==1L)
      if(draft) value.put("editorToken",text("editor_token")).put("contentRevision",number("content_revision")).put("baseRevision",number("base_revision")).put("dirty",number("dirty")==1L)
      else value.put("revision",number("revision")).put("createdAt",number("created_at")).put("lastEditor",text("last_editor")).put("lastDraftRevision",number("last_draft_revision"))
      value
    }
  }
  private fun <T> transaction(action: ()->T): T { val database=db;database.beginTransaction();try { val value=action();database.setTransactionSuccessful();return value } finally { database.endTransaction() } }
  companion object {
    private fun search(title: String,body: String)=BookRepository.normalizeTitle("$title\n$body")
    private fun validate(title: String,body: String) { require(title.length<=200 && body.length<=50000) { "Başlık 200, not metni 50.000 karakteri aşamaz" };require(!title.contains('\u0000') && !body.contains('\u0000')) }
    private const val LIST="""SELECT n.id,n.document_id,n.scope,n.page_index,n.annotation_id,substr(n.quote,1,300) quote,
      CASE WHEN d.dirty=1 AND d.deleted=0 THEN d.title ELSE n.title END title,
      substr(CASE WHEN d.dirty=1 AND d.deleted=0 THEN d.body ELSE n.body END,1,400) body,
      CASE WHEN d.dirty=1 AND d.deleted=0 THEN d.search_text ELSE n.search_text END search_text,
      n.revision,MAX(n.updated_at,COALESCE(d.updated_at,0)) updated_at,
      CASE WHEN d.dirty=1 AND d.deleted=0 THEN 1 ELSE 0 END is_draft,0 draft_only
      FROM notes n LEFT JOIN note_drafts d ON d.id=n.id WHERE n.deleted=0
      UNION ALL SELECT d.id,d.document_id,d.scope,d.page_index,d.annotation_id,substr(d.quote,1,300),d.title,substr(d.body,1,400),d.search_text,d.content_revision,d.updated_at,1,1
      FROM note_drafts d WHERE d.deleted=0 AND d.dirty=1 AND NOT EXISTS(SELECT 1 FROM notes n WHERE n.id=d.id)"""
    fun create(db: SQLiteDatabase) {
      db.execSQL("ALTER TABLE annotations ADD COLUMN note_delete_group TEXT")
      db.execSQL("""CREATE TABLE notes(id TEXT PRIMARY KEY,document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
        scope TEXT NOT NULL CHECK(scope IN ('book','page','passage')),page_index INTEGER NOT NULL,annotation_id TEXT REFERENCES annotations(id) ON DELETE SET NULL,
        quote TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL,search_text TEXT NOT NULL,revision INTEGER NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,
        deleted INTEGER NOT NULL DEFAULT 0,deletion_group TEXT,last_editor TEXT NOT NULL,last_draft_revision INTEGER NOT NULL)""")
      db.execSQL("""CREATE TABLE note_drafts(id TEXT PRIMARY KEY,document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
        scope TEXT NOT NULL CHECK(scope IN ('book','page','passage')),page_index INTEGER NOT NULL,annotation_id TEXT REFERENCES annotations(id) ON DELETE SET NULL,
        quote TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL,search_text TEXT NOT NULL,editor_token TEXT NOT NULL,content_revision INTEGER NOT NULL,
        base_revision INTEGER NOT NULL,dirty INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,deleted INTEGER NOT NULL DEFAULT 0,deletion_group TEXT)""")
      db.execSQL("CREATE INDEX notes_book_date ON notes(document_id,deleted,updated_at DESC,id)")
      db.execSQL("CREATE INDEX drafts_book_date ON note_drafts(document_id,deleted,dirty,updated_at DESC,id)")
      db.execSQL("CREATE INDEX notes_passage ON notes(document_id,annotation_id,deleted)")
      db.execSQL("CREATE INDEX drafts_passage ON note_drafts(document_id,annotation_id,deleted)")
      db.execSQL("CREATE UNIQUE INDEX notes_one_passage ON notes(document_id,annotation_id) WHERE deleted=0 AND annotation_id IS NOT NULL")
    }
  }
}
