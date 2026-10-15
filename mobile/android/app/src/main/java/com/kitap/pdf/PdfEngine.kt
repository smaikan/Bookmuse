package com.kitap.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.os.CancellationSignal
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import io.legere.pdfiumandroid.api.Config
import org.json.JSONObject
import java.io.File
import java.io.Closeable
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.RejectedExecutionException

/** All PDFium calls, including close and text extraction, run on this single bounded worker. */
class PdfEngine(private val context: Context) {
  private val worker = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, EngineQueue(16)) { runnable ->
    Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); runnable.run() }, "KitapPdfWorker")
  }
  val bitmaps = ByteBudget(24L * 1024L * 1024L)
  val textBudget = ByteBudget(4L * 1024L * 1024L)
  private var selection: Selection? = null
  private val thumbnails = ByteBudget(2L * 1024L * 1024L)
  private val thumbnailDirectory = File(context.cacheDir,"thumbnails")
  private var thumbnailRecovered=false
  private val nextId = AtomicLong()
  private val closeRequested = AtomicBoolean(false)
  private var document: PdfDocument? = null
  private var session = ""
  private var bookId = ""
  internal val books by lazy { BookRepository(context) }
  private val core by lazy { PdfiumCore(context, Config(pageRetentionCount = 1)) }

  fun execute(priority: Int = 0, action: () -> Unit) {
    worker.execute(EngineQueue.Work(priority) {
      android.os.Process.setThreadPriority(if (priority < 2) 2 else android.os.Process.THREAD_PRIORITY_BACKGROUND)
      if (closeRequested.getAndSet(false)) close()
      try { action() } finally { if (closeRequested.getAndSet(false)) close() }
    })
  }

  fun requestClose() {
    closeRequested.set(true)
    try { execute { } } catch (_: RejectedExecutionException) {
      // A running or already queued action will consume the coalesced teardown request.
    }
  }

  fun openSample(): String {
    close()
    val file = SamplePdf.create(context)
    document = openDocument(file)
    try {
      session = nextId.incrementAndGet().toString()
      val doc = checkNotNull(document)
      val text = checkNotNull(doc.openPage(0)).use { page ->
        page.openTextPage().use { textPage ->
          val count = textPage.textPageCountChars()
          require(count in 1..20_000) { "Örnek metin beklenen sınırda değil" }
          val quote = textPage.textPageGetText(0, count).orEmpty()
          val box = textPage.textPageGetCharBox(0)
          val hitIndex = box?.let { textPage.textPageGetCharIndexAtPos(it.centerX().toDouble(), it.centerY().toDouble(), 2.0, 2.0) } ?: -1
          JSONObject().put("quote", quote).put("characterCount", count).put("hitIndex", hitIndex)
            .put("turkishTextMatches", quote.contains(SamplePdf.EXPECTED))
        }
      }
      return JSONObject().put("sessionId", session).put("pageCount", doc.getPageCount()).put("text", text).toString()
    } catch (error: Exception) {
      try { close() } catch (closeError: Exception) { error.addSuppressed(closeError) }
      throw error
    }
  }

  fun listBooks(offset: Int, limit: Int) = books.list(offset, limit)
  fun saveOptions(id: String, options: String) = books.saveOptions(id, ReaderOptions.parse(options))
  fun queryBooks(query: String, sort: String, offset: Int, limit: Int) = books.list(offset,limit,query,sort)
  fun mostRecentBook() = books.mostRecent()
  fun renameBook(id: String,title: String) = books.rename(id,title)
  fun libraryMode() = books.libraryMode()
  fun setLibraryMode(value: String) = books.updateLibraryMode(value)
  fun isBookmarked(id: String,page: Int) = books.isBookmarked(id,page)
  fun setBookmark(id: String,page: Int,value: Boolean) = books.setBookmark(id,page,value)
  fun bookmarks(id: String,offset: Int,limit: Int) = books.bookmarks(id,offset,limit)
  fun annotations(id: String,query: String,kind: String,color: String,sort: String,offset: Int,limit: Int) = books.annotations.list(id,query,kind,color,sort,offset,limit)
  fun changeAnnotation(id: String,mark: String,revision: Int,kind: String,color: String,deleted: Boolean) = books.annotations.change(id,mark,revision,kind,color,deleted)
  fun annotation(id: String,mark: String) = (books.annotations.detail(id,mark) ?: error("İşaret bulunamadı")).toString()
  fun openNote(id: String,note: String,scope: String,page: Int,annotation: String)=books.notes.open(id,note,scope,page,annotation)
  fun writeNote(id: String,note: String,token: String,revision: Int,title: String,body: String)=books.notes.write(id,note,token,revision,title,body)
  fun saveNote(id: String,note: String,token: String,revision: Int,title: String,body: String)=books.notes.save(id,note,token,revision,title,body)
  fun endNote(id: String,note: String,token: String,revision: Int,title: String,body: String)=books.notes.end(id,note,token,revision,title,body)
  fun notes(id: String,query: String,scope: String,sort: String,offset: Int,limit: Int)=books.notes.list(id,query,scope,sort,offset,limit)
  fun deleteNote(id: String,note: String,revision: Int,draftOnly: Boolean,deleted: Boolean)=books.notes.changeDeleted(id,note,revision,draftOnly,deleted)
  fun deleteAnnotation(id: String,mark: String,revision: Int,deleteNotes: Boolean)=books.annotations.deleteWithNotes(id,mark,revision,deleteNotes)
  fun selectionNote(id: String,token: String,operation: String,color: String,manualText: String): String {
    return books.createPassageNote(selectedAnchor(id,token,manualText),operation,color)
  }

  fun prepareText(id: String,index: Int): TextGeometry {
    check(id==session && id.isNotEmpty()) { "Kitap oturumu değişti" }
    return checkNotNull(document).openPage(index)!!.use { page -> page.openTextPage().use { text ->
      val count=text.textPageCountChars()
      require(count in 0..32000) { "Bu sayfanın metin katmanı bellek sınırını aşıyor. Bölge işaretle kullanılabilir." }
      val lease=textBudget.reserve(count.toLong()*19*4+16_384) ?: error("Metin belleği meşgul. Daha kısa bir seçim deneyin.")
      try {
        val packed=if(count==0) FloatArray(0) else PdfGeometry.glyphs(page,text,count) ?: error("Metin geometrisi bu motor sürümünde desteklenmiyor. Bölge işaretle kullanılabilir.")
        check(packed.size==count*19) { "Metin geometrisi desteklenmiyor" }
        TextGeometry(packed,lease)
      } catch(error: Throwable) { lease.close();throw error }
    } }
  }
  fun select(id: String,index: Int,metadata: TextGeometry,start: Int,end: Int,token: String): String {
    check(id==session && id.isNotEmpty()) { "Kitap oturumu değişti" }
    val quads=metadata.ranges(start,end,false)
    return checkNotNull(document).openPage(index)!!.use { page -> page.openTextPage().use { text ->
      require(text.textPageCountChars()==metadata.count) { "Metin katmanı değişti" }
      val quote=text.textPageGetText(start,end-start+1).orEmpty()
      require(quote.isNotBlank() && !quote.contains('\uFFFD') && !quote.contains('\u0000')) { "Alıntı güvenilir çıkarılamadı. Bölge işaretle kullanılabilir." }
      val prefix=text.textPageGetText(maxOf(0,start-40),minOf(40,start)).orEmpty().takeLast(100)
      val suffixCount=minOf(40,metadata.count-end-1)
      val suffix=if(suffixCount>0) text.textPageGetText(end+1,suffixCount).orEmpty().take(100) else ""
      val book=books.find(bookId) ?: error("Kitap bulunamadı")
      remember(token,AnnotationRepository.Anchor(bookId,book.fingerprint,index,quote,prefix,suffix,start,end,pageBoxes(page),quads))
    } }
  }
  fun selectRegion(id: String,index: Int,rect: FloatArray,token: String): String {
    check(id==session && id.isNotEmpty());require(rect.size==4 && rect.all { it.isFinite() && it in 0f..1f } && rect[2]>rect[0] && rect[3]>rect[1])
    return checkNotNull(document).openPage(index)!!.use { page ->
      val points=listOf(rect[0] to rect[3],rect[2] to rect[3],rect[2] to rect[1],rect[0] to rect[1])
      val quad=FloatArray(8)
      points.forEachIndexed { i,p -> val pdf=page.mapDeviceCoordsToPage(0,0,1_000_000,1_000_000,0,(p.first*1_000_000).toInt(),(p.second*1_000_000).toInt());quad[i*2]=pdf.x;quad[i*2+1]=pdf.y }
      val book=books.find(bookId) ?: error("Kitap bulunamadı")
      remember(token,AnnotationRepository.Anchor(bookId,book.fingerprint,index,"","","",-1,-1,pageBoxes(page),listOf(quad),true))
    }
  }
  private fun pageBoxes(page: io.legere.pdfiumandroid.PdfPage): String {
    fun box(rect: android.graphics.RectF)=org.json.JSONArray(listOf(rect.left,rect.top,rect.right,rect.bottom))
    return JSONObject().put("media",box(page.getPageMediaBox())).put("crop",box(page.getPageCropBox())).put("rotation",page.getPageRotation()).toString()
  }
  private fun remember(token: String,anchor: AnnotationRepository.Anchor): String {
    anchor.validate();selection?.close();selection=null
    val lease=textBudget.reserve(131_072) ?: error("Seçim bellek sınırına sığmıyor")
    selection=Selection(session,token,anchor,lease)
    return JSONObject().put("id",token).put("pageIndex",anchor.page).put("quote",anchor.quote)
      .put("lineCount",if(anchor.region) 0 else anchor.quads.size).put("region",anchor.region).toString()
  }
  fun clearSelection(id: String,token: String) { if(selection?.session==id && selection?.token==token) { selection?.close();selection=null } }
  private fun selectedAnchor(id: String,token: String,manualText: String): AnnotationRepository.Anchor {
    val chosen=selection ?: error("Seçim değişti; yeniden seçin")
    require(chosen.session==id && id==session && chosen.token==token && chosen.anchor.document==bookId) { "Seçim veya kitap oturumu değişti" }
    require(manualText.length<=1000)
    return if(chosen.anchor.region) chosen.anchor.copy(quote=manualText.trim()) else chosen.anchor
  }
  fun saveSelection(id: String,token: String,kind: String,color: String,operation: String,manualText: String): String {
    // Preserve the selection on both failure and success until UI explicitly acknowledges commit.
    return books.annotations.save(selectedAnchor(id,token,manualText),operation,kind,color)
  }
  fun overlays(id: String,index: Int): Overlay {
    check(id==session && id.isNotEmpty());var lease: ByteBudget.Lease?=null
    try {
      return checkNotNull(document).openPage(index)!!.use { page ->
        var count=0;var coords=FloatArray(0);var colors=IntArray(0);var kinds=IntArray(0)
        books.annotations.ranges(bookId,index) { c ->
          require(c.count<=8192) { "Bu sayfada çok fazla işaret geometrisi var" }
          count=c.count;lease=textBudget.reserve(count*128L+4096) ?: error("İşaret belleği meşgul");coords=FloatArray(count*8);colors=IntArray(count);kinds=IntArray(count)
          while(c.moveToNext()) {
            val row=c.position;colors[row]=android.graphics.Color.parseColor(c.getString(1));kinds[row]=when(c.getString(0)) { "underline"->0;"highlight"->1;else->2 }
            repeat(4) { corner -> val point=page.mapPageCoordsToDevice(0,0,1_000_000,1_000_000,0,c.getDouble(2+corner*2),c.getDouble(3+corner*2));coords[row*8+corner*2]=point.x/1_000_000f;coords[row*8+corner*2+1]=point.y/1_000_000f }
          }
        }
        Overlay(coords,colors,kinds,checkNotNull(lease))
      }
    } catch(error: Throwable) { lease?.close();throw error }
  }
  fun annotationFocus(id: String,mark: String,index: Int): FloatArray {
    check(id==session);val detail=books.annotations.detail(bookId,mark) ?: error("İşaret bulunamadı")
    require(!detail.getBoolean("deleted") && detail.getInt("pageIndex")==index)
    return checkNotNull(document).openPage(detail.getInt("pageIndex"))!!.use { page ->
      books.readableDatabase.rawQuery("SELECT c0,c1,c2,c3,c4,c5,c6,c7 FROM annotation_ranges WHERE annotation_id=? ORDER BY ordinal LIMIT 1",arrayOf(mark)).use { c ->
        check(c.moveToFirst());val x=(c.getDouble(0)+c.getDouble(2)+c.getDouble(4)+c.getDouble(6))/4
        val y=(c.getDouble(1)+c.getDouble(3)+c.getDouble(5)+c.getDouble(7))/4
        val point=page.mapPageCoordsToDevice(0,0,1_000_000,1_000_000,0,x,y)
        floatArrayOf(point.x/1_000_000f,point.y/1_000_000f)
      }
    }
  }
  class Overlay(val quads: FloatArray,val colors: IntArray,val kinds: IntArray,private val lease: ByteBudget.Lease): AutoCloseable {
    // Build once on the serial worker; Winding fills overlaps only once.
    val highlightPath=android.graphics.Path().apply {
      fillType=android.graphics.Path.FillType.WINDING
      for(i in kinds.indices) if(kinds[i]==1) {
        val o=i*8;var area=0f;repeat(4) { c -> val n=(c+1)%4;area+=quads[o+c*2]*quads[o+n*2+1]-quads[o+n*2]*quads[o+c*2+1] }
        val order=if(area>=0) intArrayOf(0,1,2,3) else intArrayOf(0,3,2,1)
        moveTo(quads[o],quads[o+1]);for(c in 1..3) lineTo(quads[o+order[c]*2],quads[o+order[c]*2+1]);close()
      }
    }
    override fun close()=lease.close()
  }
  private class Selection(val session: String,val token: String,val anchor: AnnotationRepository.Anchor,private val lease: ByteBudget.Lease): AutoCloseable { override fun close()=lease.close() }

  fun importBook(uri: Uri, relinkId: String, operation: ImportOperation): String {
    // Import is a library action. Release any previous session before validation,
    // including a teardown command that could not enter a previously full queue.
    close()
    books.recover()
    operation.check()
    val existing = books.byUri(uri.toString())
    val original = if (relinkId.isNotEmpty()) books.find(relinkId) ?: error("Kitap bulunamadı") else null
    require(relinkId.isEmpty() || existing == null || existing.id == relinkId) { "Bu dosya başka bir kitaba bağlı" }
    val filename = "${UUID.randomUUID()}.pdf"
    val target = File(books.directory, filename)
    var committed = false
    try {
      var title = "PDF kitabı"
      val signal = CancellationSignal()
      operation.attach(Closeable { signal.cancel() })
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null, signal)?.use {
        if (it.moveToFirst() && !it.isNull(0)) title = it.getString(0).trim().take(300).ifBlank { "PDF kitabı" }
      }
      operation.check()
      val asset = context.contentResolver.openAssetFileDescriptor(uri, "r", signal) ?: error("Dosya sağlayıcısı okunamadı")
      val copy = asset.use { descriptor ->
        descriptor.createInputStream().use { AtomicPdfCopy.copy(it, target, operation) }
      }
      operation.check()
      if (original != null) {
        require(copy.hash == original.fingerprint && copy.bytes == original.size) {
          "Bu PDF aynı kitap değil. Farklı dosyayı yeni kitap olarak ekleyin; eski konum korundu."
        }
      }
      // Validate one document, not every page. This uses the same serial PDFium worker as reading.
      val count = openDocument(target).use { doc ->
        val pages = doc.getPageCount()
        require(pages in 1..1_000_000) { "PDF boş veya bir milyon sayfa sınırını aşıyor" }
        checkNotNull(doc.openPage(0)).use { page ->
          require(page.getPageWidthPoint() > 0 && page.getPageHeightPoint() > 0) { "PDF sayfası geçersiz" }
        }
        pages
      }
      operation.check()
      if (original != null) {
        require(count == original.pageCount) { "Kitabın sayfa sayısı değişmiş; eski kayıt korundu" }
        operation.commit { books.relink(original.id, uri.toString(), filename) }
        committed = true
        books.file(original).delete()
        return original.json().toString()
      }
      val duplicate=books.byFingerprint(copy.hash,copy.bytes,count)
      if(duplicate!=null) {
        val oldFile=books.file(duplicate)
        if(!oldFile.isFile || oldFile.length()!=duplicate.size) {
          operation.commit { books.relink(duplicate.id,uri.toString(),filename) };committed=true
          oldFile.delete()
        } else operation.commit { }
        return duplicate.json().put("duplicate",true).toString()
      }
      require(existing==null) { "Bu kaynak artık farklı bir PDF içeriyor. Yeni dosyayı başka konumdan seçin; eski kayıtlar korundu." }
      val book = BookRepository.Book(UUID.randomUUID().toString(), uri.toString(), filename,
        title, copy.hash, copy.bytes, count, System.currentTimeMillis())
      operation.commit { books.add(book) }
      committed = true
      return book.json().put("duplicate", false).toString()
    } finally { if (!committed) target.delete() }
  }

  fun openBook(id: String): String {
    close()
    val book = books.find(id) ?: error("Kitap bulunamadı")
    val file = books.file(book)
    check(file.isFile) { "Kitabın yerel kopyası bulunamadı. Dosyayı yeniden bağlayın." }
    check(file.length() == book.size) { "Kitabın kopyası değişmiş. Aynı PDF'yi yeniden bağlayın." }
    document = openDocument(file)
    try {
      check(checkNotNull(document).getPageCount() == book.pageCount) { "PDF sayfa bilgisi değişmiş" }
      PageIndex.requireValid(book.pageIndex, book.pageCount)
      bookId = id
      session = nextId.incrementAndGet().toString()
      return book.json().put("sessionId", session).put("options", books.readerDefaults().json()).toString()
    } catch (error: Exception) { close(); throw error }
  }

  fun removeBook(id: String) {
    if (bookId == id) close()
    books.remove(id)
  }

  private data class Presented(val session: String,val page: Int)
  private val lastPresented=java.util.concurrent.atomic.AtomicReference<Presented?>(null)
  // Called on the drawing thread only after a complete presentation. No DB/I/O here.
  fun recordPresentedPage(id: String,page: Int) { lastPresented.set(Presented(id,page)) }
  fun flushPosition(id: String? = null) {
    if(id!=null && id!=session) return
    val shown=lastPresented.get() ?: return
    if(shown.session==session && bookId.isNotEmpty()) { books.savePage(bookId,shown.page);lastPresented.compareAndSet(shown,null) }
  }
  fun saveDisplayedPage(id: String, page: Int) {
    // A callback from a removed/switched view must never write another book's position.
    if (id != session || bookId.isEmpty()) return
    books.savePage(bookId, page)
    lastPresented.get()?.takeIf { it.session==id && it.page==page }?.let { lastPresented.compareAndSet(it,null) }
  }

  fun closeSession(id: String) { if (session == id) close() }

  fun render(id: String, pageIndex: Int, viewWidth: Int, viewHeight: Int, pixelLimit: Int = 1_500_000): Frame? {
    if (session != id || id.isEmpty()) return null
    val doc = document ?: return null
    val started = SystemClock.elapsedRealtimeNanos()
    PageIndex.requireValid(pageIndex, doc.getPageCount())
    return checkNotNull(doc.openPage(pageIndex)).use { page ->
      val size = RenderSize.fit(page.getPageWidthPoint(), page.getPageHeightPoint(), viewWidth, viewHeight, pixelLimit.coerceIn(100_000,1_500_000).toLong())
      val lease = bitmaps.reserve(size.bytes) ?: error("Görüntü bellek bütçesine sığmıyor")
      try {
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        check(bitmap.allocationByteCount.toLong() <= lease.bytes) { "Bitmap allocation budget mismatch" }
        page.renderPageBitmap(bitmap, 0, 0, size.width, size.height, renderAnnot = false)
        Frame(bitmap, lease, (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0)
      } catch (error: Throwable) {
        lease.close()
        throw error
      }
    }
  }

  fun renderTile(id: String, pageIndex: Int, tile: TileLayout.Tile): Frame? {
    if (id != session || id.isEmpty()) return null
    val doc = document ?: return null
    PageIndex.requireValid(pageIndex, doc.getPageCount())
    val lease = bitmaps.reserve(tile.width.toLong() * tile.height * 4) ?: return null
    try {
      val started = SystemClock.elapsedRealtimeNanos()
      val bitmap = Bitmap.createBitmap(tile.width, tile.height, Bitmap.Config.ARGB_8888)
      check(bitmap.allocationByteCount.toLong() <= lease.bytes)
      checkNotNull(doc.openPage(pageIndex)).use { page ->
        page.renderPageBitmap(bitmap, -tile.x, -tile.y, tile.pageW, tile.pageH, renderAnnot = false)
      }
      return Frame(bitmap, lease, (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0)
    } catch (error: Throwable) { lease.close(); throw error }
  }

  fun renderThumbnail(id: String, pageIndex: Int): Frame? {
    val book=books.find(id) ?: return null
    PageIndex.requireValid(pageIndex,book.pageCount)
    check(thumbnailDirectory.isDirectory || thumbnailDirectory.mkdirs())
    if(!thumbnailRecovered) {
      thumbnailDirectory.listFiles()?.filter { it.isFile && it.name.endsWith(".part") }?.forEach { it.delete() }
      thumbnailRecovered=true
    }
    val key="${book.fingerprint}-$pageIndex-160.jpg"
    val file=File(thumbnailDirectory,key)
    var rendered: Frame? = null
    if(file.isFile) {
      val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeFile(file.path,bounds)
      if(bounds.outWidth in 1..160 && bounds.outHeight in 1..224) {
        val bytes=bounds.outWidth.toLong()*bounds.outHeight*4
        val lease=bitmaps.reserve(bytes) ?: return null
        val thumbLease=thumbnails.reserve(bytes) ?: run { lease.close();return null }
        try {
          val bitmap=BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply { inPreferredConfig=Bitmap.Config.ARGB_8888 })
          if(bitmap!=null && bitmap.allocationByteCount<=bytes) rendered=Frame(bitmap,lease,0.0,thumbLease)
          else { lease.close();thumbLease.close() }
        } catch(error: Throwable) { lease.close();thumbLease.close();throw error }
      }
      if(rendered==null) { file.delete();books.forgetThumbnail(key) }
    }
    if(rendered==null) {
      // Library views are disabled on blur. Never parse a second book beside an active reader.
      if(document!=null && bookId!=id) return null
      val temporaryDocument=if(document==null) openDocument(books.file(book)) else null
      try {
        val doc=temporaryDocument ?: document ?: return null
        rendered=checkNotNull(doc.openPage(pageIndex)).use { page ->
          val size=RenderSize.fit(page.getPageWidthPoint(),page.getPageHeightPoint(),160,224,35_840)
          val lease=bitmaps.reserve(size.bytes) ?: return null
          val thumbLease=thumbnails.reserve(size.bytes) ?: run { lease.close();return null }
          try {
            val bitmap=Bitmap.createBitmap(size.width,size.height,Bitmap.Config.ARGB_8888)
            page.renderPageBitmap(bitmap,0,0,size.width,size.height,renderAnnot=false)
            Frame(bitmap,lease,0.0,thumbLease)
          } catch(error: Throwable) { lease.close();thumbLease.close();throw error }
        }
      } finally { temporaryDocument?.close() }
      val part=File(thumbnailDirectory,"$key.part")
      try {
        FileOutputStream(part).use { stream -> check(rendered!!.bitmap.compress(Bitmap.CompressFormat.JPEG,70,stream)); stream.fd.sync() }
        check(part.renameTo(file)); books.touchThumbnail(key,file.length())
      } catch(error: Exception) { part.delete();file.delete();rendered?.close();throw error }
    }
    try {
      books.touchThumbnail(key,file.length())
      while(books.thumbnailBytes()>64L*1024*1024) {
        val oldest=books.oldestThumbnails();if(oldest.isEmpty()) break
        oldest.forEach { cachedKey -> File(thumbnailDirectory,cachedKey).delete();books.forgetThumbnail(cachedKey) }
      }
    } catch(error: Exception) { rendered?.close();throw error }
    return rendered
  }

  fun lifecycleProbe(): String {
    close()
    val started = SystemClock.elapsedRealtime()
    var matches = 0
    val file = SamplePdf.create(context)
    repeat(20) {
        openDocument(file).use { doc ->
          checkNotNull(doc.openPage(0)).use { page ->
            page.openTextPage().use { text ->
              val count = text.textPageCountChars()
              require(count in 1..20_000)
              if (text.textPageGetText(0, count).orEmpty().contains(SamplePdf.EXPECTED)) matches++
            }
          }
        }
    }
    return JSONObject().put("iterations", 20).put("textMatches", matches)
      .put("elapsedMs", SystemClock.elapsedRealtime() - started).put("ownedBitmapBytes", bitmaps.used).toString()
  }

  fun close() {
    flushPosition()
    selection?.close();selection=null
    session = ""
    bookId = ""
    val owned = document
    document = null
    owned?.close()
  }

  private fun openDocument(file: File): PdfDocument {
    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try { return core.newDocument(fd) }
    catch (error: Exception) { fd.close(); throw error }
    // PdfiumAndroidKt's document owns and closes the descriptor after a successful open.
  }

  class Frame(val bitmap: Bitmap, private val lease: ByteBudget.Lease, val renderMs: Double, private val secondary: ByteBudget.Lease? = null) : AutoCloseable {
    // A previously drawn bitmap may still be referenced by Android RenderThread; do not recycle it.
    override fun close() { lease.close();secondary?.close() }
  }

  companion object {
    @Volatile private var instance: PdfEngine? = null
    fun get(context: Context): PdfEngine = instance ?: synchronized(this) {
      instance ?: PdfEngine(context.applicationContext).also { instance = it }
    }
  }
}
