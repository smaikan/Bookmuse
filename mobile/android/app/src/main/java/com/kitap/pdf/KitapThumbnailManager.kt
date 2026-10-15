package com.kitap.pdf

import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.KitapThumbnailManagerDelegate
import com.facebook.react.viewmanagers.KitapThumbnailManagerInterface
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class KitapThumbnailView(context: ThemedReactContext) : View(context) {
  private val engine=PdfEngine.get(context)
  private val main=Handler(Looper.getMainLooper())
  private val generation=AtomicLong()
  private var bookId="";private var page=0;private var enabled=false
  private var frame: PdfEngine.Frame?=null
  private var dark=false
  private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  fun book(value: String) { if(bookId!=value) { bookId=value;reload() } }
  fun page(value: Int) { if(page!=value) { page=value;reload() } }
  fun enabled(value: Boolean) { if(enabled!=value) { enabled=value;reload() } }
  fun dark(value: Boolean) {
    dark=value
    paint.colorFilter=if(value) ColorMatrixColorFilter(ColorMatrix(floatArrayOf(-.1567f,-.5272f,-.0531f,0f,218f,
      -.1567f,-.5272f,-.0531f,0f,218f,-.1567f,-.5272f,-.0531f,0f,218f,0f,0f,0f,1f,0f))) else null
    invalidate()
  }
  private fun release() { generation.incrementAndGet();frame?.close();frame=null;invalidate() }
  private fun reload() {
    release()
    if(!enabled || !isAttachedToWindow || bookId.isEmpty() || width<1 || height<1) return
    val token=generation.get();val id=bookId;val requestedPage=page
    try { engine.execute(3) {
      // The token is checked before parsing too, so off-screen queued work is cheap to discard.
      if(token!=generation.get()) return@execute
      val rendered=try { engine.renderThumbnail(id,requestedPage) } catch(_:Exception) { null }
      main.post { if(token==generation.get() && isAttachedToWindow && enabled) { frame=rendered;invalidate() } else rendered?.close() }
    } } catch(_:RejectedExecutionException) { /* Page numbers remain usable even if a thumbnail is delayed. */ }
  }
  override fun onAttachedToWindow() { super.onAttachedToWindow();reload() }
  override fun onDetachedFromWindow() { release();super.onDetachedFromWindow() }
  override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh);reload() }
  override fun onDraw(canvas:Canvas) {
    canvas.clipRect(0,0,width,height)
    canvas.drawColor(if(dark) Color.rgb(44,48,44) else Color.rgb(217,216,205))
    val bitmap=frame?.bitmap ?: return
    val scale=min(width.toFloat()/bitmap.width,height.toFloat()/bitmap.height)
    val w=bitmap.width*scale;val h=bitmap.height*scale
    canvas.drawBitmap(bitmap,null,RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),paint)
  }
}
class KitapThumbnailManager:SimpleViewManager<KitapThumbnailView>(),KitapThumbnailManagerInterface<KitapThumbnailView> {
  private val delegate=KitapThumbnailManagerDelegate(this)
  override fun getDelegate():ViewManagerDelegate<KitapThumbnailView> =delegate
  override fun getName()="KitapThumbnail"
  override fun createViewInstance(context:ThemedReactContext)=KitapThumbnailView(context)
  override fun setBookId(view:KitapThumbnailView,value:String?)=view.book(value.orEmpty())
  override fun setPageIndex(view:KitapThumbnailView,value:Int)=view.page(value)
  override fun setEnabled(view:KitapThumbnailView,value:Boolean)=view.enabled(value)
  override fun setDark(view:KitapThumbnailView,value:Boolean)=view.dark(value)
}
