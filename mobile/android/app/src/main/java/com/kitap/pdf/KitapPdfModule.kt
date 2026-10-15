package com.kitap.pdf

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.module.annotations.ReactModule
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@ReactModule(name = KitapPdfModule.NAME)
class KitapPdfModule(private val reactContext: ReactApplicationContext) : NativeKitapPdfSpec(reactContext), ActivityEventListener, LifecycleEventListener {
  private val engine = PdfEngine.get(reactContext)
  private val active = AtomicBoolean(true)
  private val pending = AtomicReference<Pick?>(null)
  private val shared = AtomicReference<android.net.Uri?>(null)
  private val cancellationWorker = Executors.newSingleThreadExecutor()
  private val readerWindow = ReaderWindow()
  private var windowOptions: ReaderOptions? = null
  init { reactContext.addActivityEventListener(this); reactContext.addLifecycleEventListener(this) }
  override fun getName() = NAME
  override fun openSample(promise: Promise) = submit(promise) { engine.openSample() }
  override fun close(promise: Promise) = submit(promise) { engine.close(); null }
  override fun closeSession(sessionId: String, promise: Promise) = submit(promise) { engine.closeSession(sessionId); null }
  override fun runLifecycleProbe(promise: Promise) = submit(promise) { engine.lifecycleProbe() }
  override fun listBooks(offset: Double, limit: Double, promise: Promise) = submit(promise) {
    require(offset.isFinite() && limit.isFinite() && offset == offset.toInt().toDouble() && limit == limit.toInt().toDouble())
    engine.listBooks(offset.toInt(), limit.toInt())
  }
  override fun openBook(bookId: String, promise: Promise) = submit(promise) { engine.openBook(bookId) }
  override fun removeBook(bookId: String, promise: Promise) = submit(promise) { engine.removeBook(bookId); null }
  override fun importSharedPdf(promise: Promise) {
    UiThreadUtil.runOnUiThread {
      if(pending.get()!=null) { promise.resolve("null");return@runOnUiThread }
      val activity=reactContext.currentActivity
      val uri=shared.getAndSet(null) ?: activity?.intent?.let { sharedUri(it) }
      if(uri==null) { promise.resolve("null");return@runOnUiThread }
      activity?.intent=Intent(Intent.ACTION_MAIN)
      val pick=Pick(promise,"",ImportOperation());pending.set(pick)
      try { engine.execute { try { finish(pick,engine.importBook(uri,"",pick.operation)) } catch(error: Exception) { finish(pick,error=error) } } }
      catch(error: RejectedExecutionException) { finish(pick,error=error) }
    }
  }
  @Suppress("DEPRECATION")
  private fun sharedUri(intent: Intent): android.net.Uri? {
    return try {
      val uri=when(intent.action) {
        Intent.ACTION_VIEW->intent.data
        Intent.ACTION_SEND-> if(android.os.Build.VERSION.SDK_INT>=33) intent.getParcelableExtra(Intent.EXTRA_STREAM,android.net.Uri::class.java)
          else intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? android.net.Uri
        else->null
      }
      uri?.takeIf { it.scheme=="content" && intent.type=="application/pdf" }
    } catch(_: RuntimeException) { null } // Exported entry point: reject malformed/unexpected parcels.
  }
  override fun saveReaderOptions(bookId: String, options: String, promise: Promise) = submit(promise) { engine.saveOptions(bookId, options) }
  override fun queryBooks(query: String, sort: String, offset: Double, limit: Double, promise: Promise) = submit(promise) { engine.queryBooks(query,sort,integer(offset),integer(limit)) }
  override fun queryLibrary(query: String,sort: String,favoritesOnly: Boolean,offset: Double,limit: Double,promise: Promise)=submit(promise) { engine.books.list(integer(offset),integer(limit),query,sort,favoritesOnly) }
  override fun setFavorite(bookId: String,enabled: Boolean,promise: Promise)=submit(promise) { engine.books.setFavorite(bookId,enabled) }
  override fun getReaderDefaults(promise: Promise)=submit(promise) { engine.books.readerDefaults().json().toString() }
  override fun saveReaderDefaults(options: String,promise: Promise)=submit(promise) { engine.books.saveReaderDefaults(options) }
  override fun flushReadingPosition(sessionId: String,promise: Promise)=submit(promise) { engine.flushPosition(sessionId);null }
  override fun getLibraryPreferences(promise: Promise)=submit(promise) { engine.books.libraryPreferences() }
  override fun saveLibraryPreferences(sort: String,favoritesOnly: Boolean,promise: Promise)=submit(promise) { engine.books.saveLibraryPreferences(sort,favoritesOnly);null }
  override fun mostRecentBook(promise: Promise) = submit(promise) { engine.mostRecentBook() }
  override fun renameBook(bookId: String,title: String,promise: Promise) = submit(promise) { engine.renameBook(bookId,title);null }
  override fun getLibraryMode(promise: Promise) = submit(promise) { engine.libraryMode() }
  override fun setLibraryMode(mode: String,promise: Promise) = submit(promise) { engine.setLibraryMode(mode);null }
  override fun isBookmarked(bookId: String,page: Double,promise: Promise) = submit(promise) { engine.isBookmarked(bookId,integer(page)) }
  override fun setBookmark(bookId: String,page: Double,enabled: Boolean,promise: Promise) = submit(promise) { engine.setBookmark(bookId,integer(page),enabled) }
  override fun listBookmarks(bookId: String,offset: Double,limit: Double,promise: Promise) = submit(promise) { engine.bookmarks(bookId,integer(offset),integer(limit)) }
  override fun saveSelection(sessionId: String,selectionId: String,kind: String,color: String,operationId: String,manualText: String,promise: Promise) = submit(promise) { engine.saveSelection(sessionId,selectionId,kind,color,operationId,manualText) }
  override fun listAnnotations(bookId: String,query: String,kind: String,color: String,sort: String,offset: Double,limit: Double,promise: Promise) = submit(promise) { engine.annotations(bookId,query,kind,color,sort,integer(offset),integer(limit)) }
  override fun changeAnnotation(bookId: String,id: String,revision: Double,kind: String,color: String,deleted: Boolean,promise: Promise) = submit(promise) { engine.changeAnnotation(bookId,id,integer(revision),kind,color,deleted) }
  override fun newOperationId(promise: Promise) { promise.resolve(java.util.UUID.randomUUID().toString()) }
  override fun getAnnotation(bookId: String,id: String,promise: Promise) = submit(promise) { engine.annotation(bookId,id) }
  override fun openNoteEditor(bookId: String,id: String,scope: String,page: Double,annotationId: String,promise: Promise) = submit(promise) { engine.openNote(bookId,id,scope,if(page == -1.0) -1 else integer(page),annotationId) }
  override fun writeNoteDraft(bookId: String,id: String,editorToken: String,revision: Double,title: String,body: String,promise: Promise) = submit(promise) { engine.writeNote(bookId,id,editorToken,integer(revision),title,body) }
  override fun saveNote(bookId: String,id: String,editorToken: String,revision: Double,title: String,body: String,promise: Promise) = submit(promise) { engine.saveNote(bookId,id,editorToken,integer(revision),title,body) }
  override fun endNoteEditor(bookId: String,id: String,editorToken: String,revision: Double,title: String,body: String,promise: Promise) = submit(promise) { engine.endNote(bookId,id,editorToken,integer(revision),title,body);null }
  override fun listNotes(bookId: String,query: String,scope: String,sort: String,offset: Double,limit: Double,promise: Promise) = submit(promise) { engine.notes(bookId,query,scope,sort,integer(offset),integer(limit)) }
  override fun changeNoteDeleted(bookId: String,id: String,revision: Double,draftOnly: Boolean,deleted: Boolean,promise: Promise) = submit(promise) { engine.deleteNote(bookId,id,integer(revision),draftOnly,deleted) }
  override fun deleteAnnotation(bookId: String,id: String,revision: Double,deleteNotes: Boolean,promise: Promise) = submit(promise) { engine.deleteAnnotation(bookId,id,integer(revision),deleteNotes) }
  override fun selectionNote(sessionId: String,selectionId: String,operationId: String,color: String,manualText: String,promise: Promise) = submit(promise) { engine.selectionNote(sessionId,selectionId,operationId,color,manualText) }
  override fun copyText(text: String,promise: Promise) {
    if(text.length>10000) { promise.reject("TEXT_LIMIT","Alıntı çok uzun");return }
    UiThreadUtil.runOnUiThread {
      try { (reactContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Kitap alıntısı",text));promise.resolve(null) }
      catch(error: Exception) { promise.reject("CLIPBOARD","Alıntı kopyalanamadı",error) }
    }
  }
  override fun setWindowOptions(options: String, promise: Promise) {
    UiThreadUtil.runOnUiThread {
      try {
        if (options.isEmpty()) { windowOptions = null; readerWindow.restore() }
        else {
          val parsed = ReaderOptions.parse(options)
          val activity = reactContext.currentActivity ?: error("Okuma ayarı için uygulamaya dönün")
          windowOptions = parsed; readerWindow.apply(activity, parsed)
        }
        promise.resolve(null)
      } catch (error: Exception) { promise.reject("READER_WINDOW", "Pencere ayarı uygulanamadı", error) }
    }
  }
  override fun onHostPause() { readerWindow.restore();try { engine.execute { engine.flushPosition() } } catch(_:RejectedExecutionException) { } }
  override fun onHostResume() { windowOptions?.let { options -> reactContext.currentActivity?.let { readerWindow.apply(it, options) } } }
  override fun onHostDestroy() { readerWindow.restore() }

  override fun pickBook(relinkId: String, promise: Promise) {
    UiThreadUtil.runOnUiThread {
      val pick = Pick(promise, relinkId, ImportOperation())
      if (!pending.compareAndSet(null, pick)) { promise.reject("PDF_BUSY", "Bir dosya işlemi zaten sürüyor"); return@runOnUiThread }
      try {
        check(active.get())
        pick.operation.check()
        val activity = reactContext.currentActivity ?: error("Dosya seçiciyi açmak için uygulamaya dönün")
        activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
          addCategory(Intent.CATEGORY_OPENABLE)
          type = "application/pdf"
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, PICK_PDF)
      } catch (error: Exception) { finish(pick, error = error) }
    }
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode != PICK_PDF) return
    val pick = pending.get() ?: return
    val uri = data?.data
    if (resultCode != Activity.RESULT_OK || uri == null) { finish(pick, "{\"cancelled\":true}"); return }
    try {
      engine.execute {
        try {
          pick.operation.check()
          finish(pick, engine.importBook(uri, pick.relinkId, pick.operation))
        } catch (error: Exception) { finish(pick, error = error) }
      }
    } catch (error: RejectedExecutionException) { finish(pick, error = error) }
  }
  override fun onNewIntent(intent: Intent) {
    sharedUri(intent)?.let {
      shared.set(it)
      // Only availability crosses JS; the provider URI and PDF remain native.
      if(active.get()) reactContext.emitDeviceEvent("KitapSharedPdf")
    }
  }

  override fun cancelImport(promise: Promise) {
    val pick = pending.get()
    if (pick == null) { promise.resolve(false); return }
    try { cancellationWorker.execute {
      promise.resolve(pick.operation.cancel())
      // The import's finally block must finish cleanup before resolving the original promise.
    } } catch (error: RejectedExecutionException) { promise.reject("PDF_BUSY", "İptal işlemi başlatılamadı", error) }
  }

  private fun finish(pick: Pick, result: String? = null, error: Exception? = null) {
    if (!pending.compareAndSet(pick, null) || !active.get()) return
    val cancelled = try { pick.operation.check(); false } catch (_: CancellationException) { true }
    if (cancelled || error is CancellationException) pick.promise.resolve("{\"cancelled\":true}")
    else if (error == null) pick.promise.resolve(result)
    else pick.promise.reject("PDF_IMPORT", importMessage(error), error)
  }

  private fun importMessage(error: Exception): String {
    val type = error.javaClass.simpleName
    return when {
      type.contains("Password", ignoreCase = true) -> "Bu sürüm parola isteyen PDF'leri açamıyor. Parolasız bir kopya seçin."
      error is SecurityException -> "Dosyaya erişim izni alınamadı. Sistem seçicisinden yeniden seçin."
      error is java.io.IOException -> "Dosya okunamadı veya kopyalanamadı. Boş alanı ve dosya sağlayıcısını kontrol edin."
      error is IllegalArgumentException || error is IllegalStateException -> error.message ?: "PDF geçersiz"
      else -> "PDF açılamadı. Dosya bozuk, eksik veya desteklenmeyen bir PDF olabilir."
    }
  }

  private fun submit(promise: Promise, action: () -> Any?) {
    try {
      engine.execute {
        if (active.get()) {
          try { promise.resolve(action()) }
          catch (error: Exception) { promise.reject("PDF_ENGINE", importMessage(error), error) }
        }
      }
    } catch (error: RejectedExecutionException) { promise.reject("PDF_BUSY", "PDF motoru meşgul, tekrar deneyin", error) }
  }
  private fun integer(value: Double): Int {
    require(value.isFinite() && value>=0 && value<=Int.MAX_VALUE && value==value.toInt().toDouble()) { "Sayfa veya sorgu sınırı tam sayı olmalı" }
    return value.toInt()
  }

  override fun invalidate() {
    active.set(false)
    reactContext.removeActivityEventListener(this)
    reactContext.removeLifecycleEventListener(this)
    UiThreadUtil.runOnUiThread { windowOptions = null; readerWindow.restore() }
    pending.getAndSet(null)?.let { pick -> cancellationWorker.execute { pick.operation.cancel() } }
    cancellationWorker.shutdown()
    engine.requestClose()
    super.invalidate()
  }
  private data class Pick(val promise: Promise, val relinkId: String, val operation: ImportOperation)
  companion object { const val NAME = "KitapPdf"; private const val PICK_PDF = 6241 }
}
