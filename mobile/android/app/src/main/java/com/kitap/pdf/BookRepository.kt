package com.kitap.pdf

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.text.Normalizer

/** Accessed only by the serial PDF worker. Never reset the database on migration errors. */
class BookRepository(context: Context) : SQLiteOpenHelper(context, "kitap.db", null, 7) {
  val annotations by lazy { AnnotationRepository(this) }
  val notes by lazy { NoteRepository(this) }
  val directory = File(context.filesDir, "books")
  private var recovered = false
  fun createPassageNote(anchor: AnnotationRepository.Anchor,operation: String,color: String): String {
    val db=writableDatabase;db.beginTransaction()
    try {
      annotations.save(anchor,operation,if(anchor.region) "region" else "highlight",color)
      val result=notes.open(anchor.document,"","passage",anchor.page,operation)
      db.setTransactionSuccessful();return result
    } finally { db.endTransaction() }
  }

  override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL("""CREATE TABLE documents (
      id TEXT PRIMARY KEY, source_uri TEXT NOT NULL UNIQUE, filename TEXT NOT NULL UNIQUE,
      title TEXT NOT NULL, fingerprint TEXT NOT NULL, size_bytes INTEGER NOT NULL,
      page_count INTEGER NOT NULL CHECK(page_count > 0), created_at INTEGER NOT NULL)""")
    db.execSQL("""CREATE TABLE reading_positions (
      document_id TEXT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
      page_index INTEGER NOT NULL CHECK(page_index >= 0), updated_at INTEGER NOT NULL)""")
    db.execSQL("CREATE INDEX positions_recent ON reading_positions(updated_at DESC)")
    createPreferences(db)
    createNavigation(db)
    AnnotationRepository.create(db)
    NoteRepository.create(db)
    createFavorites(db)
    MarkerStyle.normalize(db)
  }
  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    require(oldVersion in 1..6 && newVersion == 7) { "Desteklenmeyen yükseltme; kayıtlar korundu" }
    if (oldVersion < 2) createPreferences(db)
    if (oldVersion < 3) createNavigation(db)
    if (oldVersion < 4) AnnotationRepository.create(db)
    if (oldVersion < 5) NoteRepository.create(db)
    if (oldVersion < 6) createFavorites(db)
    if (oldVersion < 7) MarkerStyle.normalize(db)
  }
  private fun createFavorites(db: SQLiteDatabase) {
    db.execSQL("ALTER TABLE documents ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1))")
    db.execSQL("CREATE INDEX documents_favorite ON documents(favorite)")
  }
  fun setFavorite(id: String, enabled: Boolean): Boolean {
    check(writableDatabase.update("documents",ContentValues().apply { put("favorite",if(enabled) 1 else 0) },"id=?",arrayOf(id))==1) { "Kitap bulunamadı" }
    return enabled
  }
  fun preference(key: String): String? = readableDatabase.rawQuery("SELECT value FROM app_preferences WHERE key=?",arrayOf(key))
    .use { if(it.moveToFirst()) it.getString(0) else null }
  fun preference(key: String,value: String) {
    check(writableDatabase.insertWithOnConflict("app_preferences",null,ContentValues().apply { put("key",key);put("value",value) },SQLiteDatabase.CONFLICT_REPLACE)!=-1L)
  }
  fun readerDefaults(): ReaderOptions = (preference("readerDefaults")?.let { ReaderOptions.parse(it) } ?: ReaderOptions()).copy(left=0.0,top=0.0,right=0.0,bottom=0.0,fitWidth=false,fullscreen=true,counterAlways=false)
  fun saveReaderDefaults(value: String): String {
    val normalized=ReaderOptions.parse(value).copy(left=0.0,top=0.0,right=0.0,bottom=0.0,fitWidth=false,fullscreen=true,counterAlways=false).json().toString()
    preference("readerDefaults",normalized);return normalized
  }
  fun libraryPreferences()=JSONObject().put("sort",preference("librarySort") ?: "recent").put("favoritesOnly",preference("libraryFilter")=="favorites").toString()
  fun saveLibraryPreferences(sort: String,favorites: Boolean) {
    require(sort in setOf("recent","title"));val db=writableDatabase;db.beginTransaction()
    try { preference("librarySort",sort);preference("libraryFilter",if(favorites) "favorites" else "all");db.setTransactionSuccessful() } finally { db.endTransaction() }
  }
  private fun createNavigation(db: SQLiteDatabase) {
    db.execSQL("ALTER TABLE documents ADD COLUMN title_search TEXT NOT NULL DEFAULT ''")
    db.rawQuery("SELECT id,title FROM documents", null).use { c -> while(c.moveToNext()) {
      db.update("documents", ContentValues().apply { put("title_search", normalizeTitle(c.getString(1))) }, "id=?", arrayOf(c.getString(0)))
    } }
    db.execSQL("CREATE INDEX documents_title_search ON documents(title_search)")
    db.execSQL("ALTER TABLE reading_positions ADD COLUMN has_read INTEGER NOT NULL DEFAULT 0")
    db.execSQL("UPDATE reading_positions SET has_read=1 WHERE updated_at > (SELECT created_at FROM documents WHERE id=document_id)")
    db.execSQL("""CREATE TABLE bookmarks(document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      page_index INTEGER NOT NULL CHECK(page_index>=0), created_at INTEGER NOT NULL, PRIMARY KEY(document_id,page_index))""")
    db.execSQL("CREATE TABLE app_preferences(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
    db.execSQL("CREATE TABLE thumbnail_cache(key TEXT PRIMARY KEY,bytes INTEGER NOT NULL,touched_at INTEGER NOT NULL)")
  }
  private fun createPreferences(db: SQLiteDatabase) {
    db.execSQL("""CREATE TABLE book_preferences (
      document_id TEXT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
      payload TEXT NOT NULL)""")
  }
  fun options(id: String): ReaderOptions = readableDatabase.rawQuery(
    "SELECT payload FROM book_preferences WHERE document_id=?", arrayOf(id)).use {
      if (it.moveToFirst()) ReaderOptions.parse(it.getString(0)) else ReaderOptions()
    }
  fun saveOptions(id: String, options: ReaderOptions): String {
    check(find(id) != null) { "Kitap bulunamadı" }
    val payload = options.json().toString()
    writableDatabase.insertWithOnConflict("book_preferences", null, ContentValues().apply {
      put("document_id", id); put("payload", payload)
    }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Ayarlar kaydedilemedi" } }
    return payload
  }

  fun recover() {
    if (recovered) return
    check(directory.isDirectory || directory.mkdirs()) { "Kitap klasörü hazırlanamadı" }
    val owned = mutableSetOf<String>()
    readableDatabase.rawQuery("SELECT filename FROM documents", null).use {
      while (it.moveToNext()) owned.add(it.getString(0))
    }
    // Includes a final renamed PDF left behind by process death before the DB commit.
    directory.listFiles()?.filter { it.name.endsWith(".part") || (it.name.endsWith(".pdf") && it.name !in owned) }
      ?.forEach { check(it.delete()) { "Yarım dosya temizlenemedi" } }
    recovered = true
  }

  fun list(offset: Int, limit: Int, search: String = "", sort: String = "recent", favoritesOnly: Boolean = false): String {
    recover()
    require(offset >= 0 && limit in 1..100)
    val result = JSONArray()
    require(search.length <= 200 && sort in setOf("recent", "title"))
    val pattern = "%${normalizeTitle(search).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
    val order = if(sort == "title") "d.title_search COLLATE LOCALIZED,d.id" else "COALESCE(p.updated_at,d.created_at) DESC,d.id"
    readableDatabase.rawQuery("$SELECT WHERE d.favorite>=${if(favoritesOnly) 1 else 0} AND d.title_search LIKE ? ESCAPE '\\' ORDER BY $order LIMIT ? OFFSET ?",
      arrayOf(pattern,limit.toString(), offset.toString())).use { while (it.moveToNext()) result.put(read(it).json()) }
    return result.toString()
  }
  fun find(id: String): Book? = query("d.id=?", id)
  fun byUri(uri: String): Book? = query("d.source_uri=?", uri)
  private fun query(where: String, argument: String): Book? {
    recover()
    return readableDatabase.rawQuery("$SELECT WHERE $where", arrayOf(argument)).use { if (it.moveToFirst()) read(it) else null }
  }
  fun add(book: Book) {
    writableDatabase.beginTransaction()
    try {
      writableDatabase.insertOrThrow("documents", null, values(book))
      writableDatabase.insertOrThrow("reading_positions", null, ContentValues().apply {
        put("document_id", book.id); put("page_index", 0); put("updated_at", book.createdAt)
      })
      writableDatabase.setTransactionSuccessful()
    } finally { writableDatabase.endTransaction() }
  }
  fun relink(id: String, uri: String, filename: String) {
    check(writableDatabase.update("documents", ContentValues().apply {
      put("source_uri", uri); put("filename", filename)
    }, "id=?", arrayOf(id)) == 1) { "Kitap bulunamadı" }
  }
  fun savePage(id: String, page: Int) {
    val book = find(id) ?: error("Kitap bulunamadı")
    PageIndex.requireValid(page, book.pageCount)
    check(writableDatabase.update("reading_positions", ContentValues().apply {
      put("page_index", page); put("updated_at", System.currentTimeMillis())
      put("has_read", 1)
    }, "document_id=?", arrayOf(id)) == 1) { "Okuma konumu kaydedilemedi" }
  }
  fun remove(id: String) {
    val book = find(id) ?: return
    // Delete the row first. A crash before file cleanup is recovered on the next launch.
    writableDatabase.delete("documents", "id=?", arrayOf(id))
    file(book).delete()
  }
  fun byFingerprint(hash: String,bytes: Long,pages: Int): Book? = readableDatabase.rawQuery(
    "$SELECT WHERE d.fingerprint=? AND d.size_bytes=? AND d.page_count=? ORDER BY d.created_at,d.id LIMIT 1",
    arrayOf(hash,bytes.toString(),pages.toString())).use { if(it.moveToFirst()) read(it) else null }
  fun file(book: Book): File = File(directory, book.filename)
  fun mostRecent(): String = readableDatabase.rawQuery("$SELECT WHERE p.has_read=1 ORDER BY p.updated_at DESC,d.id LIMIT 1", null)
    .use { if (it.moveToFirst()) read(it).json().toString() else "null" }
  fun rename(id: String, title: String) {
    val name=title.trim(); require(name.isNotEmpty() && name.length <= 300) { "Kitap adı 1–300 karakter olmalı" }
    check(writableDatabase.update("documents", ContentValues().apply { put("title",name); put("title_search",normalizeTitle(name)) }, "id=?", arrayOf(id)) == 1)
  }
  fun libraryMode(): String = readableDatabase.rawQuery("SELECT value FROM app_preferences WHERE key='libraryMode'",null)
    .use { if(it.moveToFirst()) it.getString(0) else "grid" }
  fun updateLibraryMode(mode: String) {
    require(mode in setOf("list","grid"))
    check(writableDatabase.insertWithOnConflict("app_preferences",null,ContentValues().apply { put("key","libraryMode");put("value",mode) },SQLiteDatabase.CONFLICT_REPLACE) != -1L)
  }
  fun isBookmarked(id: String, page: Int): Boolean = readableDatabase.rawQuery(
    "SELECT 1 FROM bookmarks WHERE document_id=? AND page_index=?",arrayOf(id,page.toString())).use { it.moveToFirst() }
  fun setBookmark(id: String, page: Int, enabled: Boolean): Boolean {
    val book=find(id) ?: error("Kitap bulunamadı"); PageIndex.requireValid(page,book.pageCount)
    if(enabled) writableDatabase.insertWithOnConflict("bookmarks",null,ContentValues().apply {
      put("document_id",id);put("page_index",page);put("created_at",System.currentTimeMillis())
    },SQLiteDatabase.CONFLICT_IGNORE).also { check(it != -1L || isBookmarked(id,page)) }
    else writableDatabase.delete("bookmarks","document_id=? AND page_index=?",arrayOf(id,page.toString()))
    return enabled
  }
  fun bookmarks(id: String, offset: Int, limit: Int): String {
    require(offset>=0 && limit in 1..100)
    val result=JSONArray()
    readableDatabase.rawQuery("SELECT page_index FROM bookmarks WHERE document_id=? ORDER BY page_index LIMIT ? OFFSET ?",
      arrayOf(id,limit.toString(),offset.toString())).use { while(it.moveToNext()) result.put(it.getInt(0)) }
    return result.toString()
  }
  fun touchThumbnail(key: String, bytes: Long) {
    writableDatabase.insertWithOnConflict("thumbnail_cache",null,ContentValues().apply {
      put("key",key);put("bytes",bytes);put("touched_at",System.currentTimeMillis())
    },SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) }
  }
  fun thumbnailBytes(): Long = readableDatabase.rawQuery("SELECT COALESCE(SUM(bytes),0) FROM thumbnail_cache",null)
    .use { it.moveToFirst();it.getLong(0) }
  fun oldestThumbnails(): List<String> = readableDatabase.rawQuery("SELECT key FROM thumbnail_cache ORDER BY touched_at,key LIMIT 32",null)
    .use { c -> buildList { while(c.moveToNext()) add(c.getString(0)) } }
  fun forgetThumbnail(key: String) { writableDatabase.delete("thumbnail_cache","key=?",arrayOf(key)) }
  private fun values(book: Book) = ContentValues().apply {
    put("id", book.id); put("source_uri", book.uri); put("filename", book.filename)
    put("title", book.title); put("fingerprint", book.fingerprint); put("size_bytes", book.size)
    put("title_search", normalizeTitle(book.title));put("favorite",if(book.favorite) 1 else 0)
    put("page_count", book.pageCount); put("created_at", book.createdAt)
  }
  private fun read(c: Cursor) = Book(c.getString(0), c.getString(1), c.getString(2), c.getString(3),
    c.getString(4), c.getLong(5), c.getInt(6), c.getLong(7), c.getInt(8), c.getInt(9)!=0)
  data class Book(val id: String, val uri: String, val filename: String, val title: String,
    val fingerprint: String, val size: Long, val pageCount: Int, val createdAt: Long, val pageIndex: Int = 0, val favorite: Boolean = false) {
    fun json() = JSONObject().put("id", id).put("title", title).put("pageCount", pageCount)
      .put("pageIndex", pageIndex).put("sizeBytes", size).put("favorite",favorite)
  }
  companion object {
    fun normalizeTitle(value: String) = Normalizer.normalize(value,Normalizer.Form.NFKC).lowercase(Locale.forLanguageTag("tr"))
    private const val SELECT = """SELECT d.id,d.source_uri,d.filename,d.title,d.fingerprint,d.size_bytes,
      d.page_count,d.created_at,COALESCE(p.page_index,0),d.favorite FROM documents d
      LEFT JOIN reading_positions p ON p.document_id=d.id"""
  }
}
