package com.kitap.pdf

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** All calls share the book repository's serial worker and migration transaction. */
class AnnotationRepository(private val books: BookRepository) {
  data class Anchor(val document: String,val fingerprint: String,val page: Int,val quote: String,
    val prefix: String,val suffix: String,val start: Int,val end: Int,val boxes: String,
    val quads: List<FloatArray>,val region: Boolean=false) {
    fun validate() {
      require(quote.length<=10000 && prefix.length<=100 && suffix.length<=100)
      require(quads.size in 1..512 && (region || start>=0 && end>=start && end-start<4096))
      quads.forEach { q ->
        require(q.size==8 && q.all { n -> n.isFinite() && kotlin.math.abs(n)<100_000_000 })
        var area=0.0;repeat(4) { i -> val j=(i+1)%4;area+=q[i*2].toDouble()*q[j*2+1]-q[j*2].toDouble()*q[i*2+1] }
        require(kotlin.math.abs(area)>1e-8) { "İşaret geometrisi boş" }
      }
      require(boxes.length<=1000)
      val meta=JSONObject(boxes);require(meta.getInt("rotation") in 0..3)
      for(name in listOf("media","crop")) { val box=meta.getJSONArray(name);require(box.length()==4 && (0..3).all { box.getDouble(it).isFinite() }) }
    }
  }
  fun save(anchor: Anchor,id: String,kind: String,color: String): String {
    validateStyle(kind,color);require(anchor.region==(kind=="region")) { "Bölge işareti gerçek metin değildir" }
    return saveCanonical(anchor,id,if(anchor.region) kind else "highlight",if(anchor.region) color else MarkerStyle.YELLOW)
  }
  private fun saveCanonical(anchor: Anchor,id: String,kind: String,color: String): String {
    anchor.validate();require(UUID.fromString(id).toString()==id);validateStyle(kind,color)
    require(anchor.region == (kind=="region")) { "Bölge işareti gerçek metin değildir" }
    val book=books.find(anchor.document) ?: error("Kitap bulunamadı")
    require(book.fingerprint==anchor.fingerprint) { "Belge sürümü değişti; işaret kaydedilmedi" }
    PageIndex.requireValid(anchor.page,book.pageCount)
    val db=books.writableDatabase;db.beginTransaction()
    try {
      val old=detail(anchor.document,id)
      if(old!=null) {
        require(old.getInt("pageIndex")==anchor.page && old.getString("quote")==anchor.quote && old.getString("kind")==kind && old.getString("color")==color && !old.getBoolean("deleted")) { "İşlem kimliği başka kayda ait" }
        require(matches(anchor,id)) { "İşlem kimliği farklı bir seçime ait" }
        db.setTransactionSuccessful();return old.toString()
      }
      val now=System.currentTimeMillis()
      db.insertOrThrow("annotations",null,ContentValues().apply {
        put("id",id);put("document_id",anchor.document);put("fingerprint",anchor.fingerprint)
        put("page_index",anchor.page);put("quote",anchor.quote);put("quote_search",BookRepository.normalizeTitle(anchor.quote))
        put("prefix",anchor.prefix);put("suffix",anchor.suffix);put("char_start",anchor.start);put("char_end",anchor.end)
        put("boxes",anchor.boxes);put("engine_version","pdfiumandroid-2.0.3/geometry-1")
        put("kind",kind);put("color",color);put("line_count",if(anchor.region) 0 else anchor.quads.size)
        put("provenance",if(anchor.region) "manual-region" else "pdf-text")
        put("created_at",now);put("updated_at",now)
      })
      anchor.quads.forEachIndexed { ordinal,q -> db.insertOrThrow("annotation_ranges",null,ContentValues().apply {
        put("annotation_id",id);put("ordinal",ordinal);q.forEachIndexed { i,n -> put("c$i",n) }
      }) }
      val result=checkNotNull(detail(anchor.document,id));db.setTransactionSuccessful();return result.toString()
    } finally { db.endTransaction() }
  }
  private fun matches(anchor: Anchor,id: String): Boolean {
    val db=books.readableDatabase
    val same=db.rawQuery("SELECT fingerprint,prefix,suffix,char_start,char_end,boxes FROM annotations WHERE document_id=? AND id=?",arrayOf(anchor.document,id)).use { c ->
      c.moveToFirst() && c.getString(0)==anchor.fingerprint && c.getString(1)==anchor.prefix && c.getString(2)==anchor.suffix && c.getInt(3)==anchor.start && c.getInt(4)==anchor.end && c.getString(5)==anchor.boxes
    }
    if(!same) return false
    return db.rawQuery("SELECT c0,c1,c2,c3,c4,c5,c6,c7 FROM annotation_ranges WHERE annotation_id=? ORDER BY ordinal",arrayOf(id)).use { c ->
      if(c.count!=anchor.quads.size) false else { var equal=true;while(c.moveToNext()) repeat(8) { if(c.getFloat(it)!=anchor.quads[c.position][it]) equal=false };equal }
    }
  }
  fun list(book: String,query: String,kind: String,color: String,sort: String,offset: Int,limit: Int): String {
    require(query.length<=200 && kind in setOf("","underline","highlight","region") && (color.isEmpty() || color.matches(Regex("#[0-9a-fA-F]{6}"))))
    require(sort in setOf("page","date") && offset>=0 && limit in 1..100)
    val pattern="%${BookRepository.normalizeTitle(query).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")}%"
    val order=if(sort=="date") "created_at DESC,id" else "page_index,created_at,id"
    val result=JSONArray()
    books.readableDatabase.rawQuery("${SELECT.replace("page_index,quote,","page_index,substr(quote,1,400),")} WHERE document_id=? AND deleted=0 AND quote_search LIKE ? ESCAPE '\\' AND (?='' OR kind=?) AND (?='' OR color=?) ORDER BY $order LIMIT ? OFFSET ?",
      arrayOf(book,pattern,kind,kind,color,color,limit.toString(),offset.toString())).use { while(it.moveToNext()) result.put(read(it)) }
    return result.toString()
  }
  fun detail(book: String,id: String): JSONObject? = books.readableDatabase.rawQuery("$SELECT WHERE document_id=? AND id=?",arrayOf(book,id))
    .use { if(it.moveToFirst()) read(it).put("noteCount",books.notes.relatedCount(book,id)) else null }
  fun change(book: String,id: String,revision: Int,kind: String,color: String,deleted: Boolean): String {
    validateStyle(kind,color)
    return changeCanonical(book,id,revision,if(kind=="region") kind else "highlight",if(kind=="region") color else MarkerStyle.YELLOW,deleted)
  }
  private fun changeCanonical(book: String,id: String,revision: Int,kind: String,color: String,deleted: Boolean): String {
    validateStyle(kind,color);require(revision>0)
    val db=books.writableDatabase;db.beginTransaction()
    try {
      val old=detail(book,id) ?: error("İşaret bulunamadı")
      require(old.getInt("revision")==revision) { "Kayıt değişti. Listeyi yenileyin." }
      require((old.getString("provenance")=="manual-region")== (kind=="region")) { "Bölge ve metin türleri birbirine dönüştürülemez" }
      if(!deleted && old.getBoolean("deleted")) books.notes.restoreRelated(book,id)
      check(db.update("annotations",ContentValues().apply {
        put("kind",kind);put("color",color);put("deleted",if(deleted) 1 else 0);put("revision",revision+1);put("updated_at",System.currentTimeMillis())
      },"document_id=? AND id=? AND revision=?",arrayOf(book,id,revision.toString()))==1)
      val result=checkNotNull(detail(book,id));db.setTransactionSuccessful();return result.toString()
    } finally { db.endTransaction() }
  }
  fun deleteWithNotes(book: String,id: String,revision: Int,deleteNotes: Boolean): String {
    val db=books.writableDatabase;db.beginTransaction()
    try {
      val old=detail(book,id) ?: error("İşaret bulunamadı")
      require(!old.getBoolean("deleted")) { "İşaret zaten silinmiş" }
      val result=change(book,id,revision,old.getString("kind"),old.getString("color"),true)
      if(deleteNotes) {
        val group=UUID.randomUUID().toString()
        books.notes.deleteRelated(book,id,group)
        db.update("annotations",ContentValues().apply { put("note_delete_group",group) },"document_id=? AND id=?",arrayOf(book,id))
      }
      db.setTransactionSuccessful();return result
    } finally { db.endTransaction() }
  }
  // A bounded native-only cursor. UI receives cards, never all range geometry.
  fun ranges(book: String,page: Int,action: (Cursor)->Unit) {
    val owner=books.find(book) ?: error("Kitap bulunamadı")
    books.readableDatabase.rawQuery("SELECT a.kind,a.color,r.c0,r.c1,r.c2,r.c3,r.c4,r.c5,r.c6,r.c7,a.id FROM annotations a JOIN annotation_ranges r ON r.annotation_id=a.id WHERE a.document_id=? AND a.fingerprint=? AND a.page_index=? AND a.deleted=0 ORDER BY a.created_at,a.id,r.ordinal LIMIT 8193",
      arrayOf(book,owner.fingerprint,page.toString())).use(action)
  }
  private fun read(c: Cursor) = JSONObject().put("id",c.getString(0)).put("pageIndex",c.getInt(1))
    .put("quote",c.getString(2)).put("kind",c.getString(3)).put("color",c.getString(4))
    .put("lineCount",c.getInt(5)).put("provenance",c.getString(6)).put("createdAt",c.getLong(7))
    .put("revision",c.getInt(8)).put("deleted",c.getInt(9)!=0)
  companion object {
    private const val SELECT="SELECT id,page_index,quote,kind,color,line_count,provenance,created_at,revision,deleted FROM annotations"
    fun validateStyle(kind: String,color: String) { require(kind in setOf("underline","highlight","region") && color.matches(Regex("#[0-9a-fA-F]{6}"))) }
    fun create(db: SQLiteDatabase) {
      db.execSQL("""CREATE TABLE annotations(id TEXT PRIMARY KEY,document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
        fingerprint TEXT NOT NULL,page_index INTEGER NOT NULL CHECK(page_index>=0),quote TEXT NOT NULL,quote_search TEXT NOT NULL,
        prefix TEXT NOT NULL,suffix TEXT NOT NULL,char_start INTEGER NOT NULL,char_end INTEGER NOT NULL,boxes TEXT NOT NULL,engine_version TEXT NOT NULL,
        kind TEXT NOT NULL CHECK(kind IN ('underline','highlight','region')),color TEXT NOT NULL,line_count INTEGER NOT NULL,provenance TEXT NOT NULL,
        created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,revision INTEGER NOT NULL DEFAULT 1,deleted INTEGER NOT NULL DEFAULT 0)""")
      db.execSQL("CREATE INDEX annotation_page ON annotations(document_id,deleted,page_index,created_at,id)")
      db.execSQL("CREATE INDEX annotation_date ON annotations(document_id,deleted,created_at,id)")
      db.execSQL("CREATE TABLE annotation_ranges(annotation_id TEXT NOT NULL REFERENCES annotations(id) ON DELETE CASCADE,ordinal INTEGER NOT NULL,c0 REAL NOT NULL,c1 REAL NOT NULL,c2 REAL NOT NULL,c3 REAL NOT NULL,c4 REAL NOT NULL,c5 REAL NOT NULL,c6 REAL NOT NULL,c7 REAL NOT NULL,PRIMARY KEY(annotation_id,ordinal))")
    }
  }
}
