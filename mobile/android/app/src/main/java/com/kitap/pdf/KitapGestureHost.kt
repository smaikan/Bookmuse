package com.kitap.pdf

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.KitapGestureHostManagerDelegate
import com.facebook.react.viewmanagers.KitapGestureHostManagerInterface
import com.facebook.react.views.view.ReactViewGroup

/** One owner for multi-touch across both PDF panes; single-touch selection stays local. */
class KitapGestureHost(context: ThemedReactContext): ReactViewGroup(context) {
  private var spanAnchor=0f
  private var lastSpan=0f
  private var overviewVisible=false
  fun overviewVisible(value: Boolean) {
    if(value==overviewVisible)return;overviewVisible=value;animation?.cancel();animation=null
    val target=if(value && !reduceMotion) (1f-1f/3f)/.12f else 0f
    if(reduceMotion) { preview=target;invalidate();return }
    animation=ValueAnimator.ofFloat(preview,target).apply { duration=200;addUpdateListener { preview=it.animatedValue as Float;invalidate() };start() }
  }
  var overviewEnabled=false
  var reduceMotion=false
  fun motionReduced(value: Boolean) {
    reduceMotion=value
    if(value) { animation?.cancel();animation=null;preview=0f;invalidate() }
  }
  private val policy=OverviewPinch()
  private var eligible=false
  private var captured=false
  private var cancelled=false
  private var scaling=false
  private var eventKey=""
  private var preview=0f
  private var animation: ValueAnimator?=null
  private val location=IntArray(2)
  private fun pages()=(0 until childCount).mapNotNull { getChildAt(it) as? KitapPdfView }
  private val detector=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
      val views=pages();if(views.isEmpty() || views.any { !it.canCoordinateZoom() }) return false
      animation?.cancel();animation=null
      val active=views.firstOrNull { it.isReadingPane() } ?: views.first()
      policy.begin(active.coordinatedZoom())
      if(spanAnchor>0f)policy.scale(d.currentSpan/spanAnchor)
      lastSpan=d.currentSpan
      views.forEach { it.beginCoordinatedZoom() };scaling=true
      return true
    }
    override fun onScale(d: ScaleGestureDetector): Boolean {
      policy.scale(if(lastSpan>0f)d.currentSpan/lastSpan else d.scaleFactor);lastSpan=d.currentSpan
      pages().forEach { it.coordinateZoom(policy.displayZoom,d.focusX-it.left,d.focusY-it.top) }
      preview=if(overviewEnabled && !reduceMotion) policy.preview else 0f;invalidate()
      return true
    }
    override fun onScaleEnd(d: ScaleGestureDetector) {
      pages().forEach { it.finishCoordinatedZoom() };scaling=false
    }
  })
  private fun inside(e: MotionEvent,index: Int): Boolean {
    val root=rootView;val insets=ViewCompat.getRootWindowInsets(root) ?: return false
    val guard=insets.getInsets(WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.mandatorySystemGestures() or WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
    root.getLocationOnScreen(location)
    return GesturePolicy.inside(e.rawX-e.x+e.getX(index)-location[0],e.rawY-e.y+e.getY(index)-location[1],root.width,root.height,guard.left,guard.right,guard.top,guard.bottom)
  }
  private fun feed(e: MotionEvent) {
    val key="${e.eventTime}:${e.action}:${e.pointerCount}"
    if(key==eventKey)return
    eventKey=key;detector.onTouchEvent(e)
  }
  override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
    if(e.actionMasked==MotionEvent.ACTION_DOWN) {
      spanAnchor=0f;lastSpan=0f;eligible=inside(e,0);captured=false;cancelled=false;eventKey="";policy.cancel();preview=0f
      feed(e);return false
    }
    if(e.actionMasked==MotionEvent.ACTION_POINTER_DOWN && eligible) {
      if((0 until e.pointerCount).any { !inside(e,it) }) { eligible=false;return false }
      spanAnchor=if(e.pointerCount==2) kotlin.math.hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1)) else 0f
      captured=true;parent?.requestDisallowInterceptTouchEvent(true);feed(e);return true
    }
    return captured
  }
  override fun onTouchEvent(e: MotionEvent): Boolean {
    if(!captured)return false
    if(e.actionMasked==MotionEvent.ACTION_CANCEL)cancelled=true
    if(e.pointerCount>2 && (e.actionMasked==MotionEvent.ACTION_POINTER_DOWN || e.actionMasked==MotionEvent.ACTION_POINTER_UP)) { spanAnchor=0f;lastSpan=0f;policy.cancel() }
    feed(e)
    if(e.actionMasked==MotionEvent.ACTION_UP || e.actionMasked==MotionEvent.ACTION_CANCEL) {
      val open=overviewEnabled && policy.armed && !cancelled
      if(scaling)pages().forEach { it.finishCoordinatedZoom() }
      scaling=false;captured=false;eligible=false;parent?.requestDisallowInterceptTouchEvent(false)
      if(open) (pages().firstOrNull { it.isReadingPane() } ?: pages().firstOrNull())?.openCoordinatedOverview()
      if(!open)animatePreviewBack();policy.cancel()
    }
    return true
  }
  fun panPeers(source: KitapPdfView,dx: Float,dy: Float) { pages().filter { it!==source }.forEach { it.coordinatePan(dx,dy) } }
  fun canZoomTogether()=pages().all { it.canCoordinateZoom() }
  fun zoomTogether(source: KitapPdfView,zoom: Float,x: Float,y: Float) {
    pages().filter { it!==source && it.canCoordinateZoom() }.forEach {
      it.beginCoordinatedZoom();it.coordinateZoom(zoom,it.width*x,it.height*y);it.finishCoordinatedZoom()
    }
  }
  fun dragPeers(source: KitapPdfView,dx: Float,direction: Int) { pages().filter { it!==source }.forEach { it.coordinateDrag(dx,direction) } }
  fun endPeerDrag(source: KitapPdfView,commit: Boolean) { pages().filter { it!==source }.forEach { it.finishCoordinatedDrag(commit) } }
  fun prepareSelection(source: KitapPdfView) { pages().filter { it!==source }.forEach { it.dropSpeculation() } }
  private fun animatePreviewBack() {
    animation?.cancel()
    if(reduceMotion || preview==0f) { preview=0f;invalidate();return }
    animation=ValueAnimator.ofFloat(preview,0f).apply { duration=160;addUpdateListener { preview=it.animatedValue as Float;invalidate() };start() }
  }
  override fun dispatchDraw(canvas: Canvas) {
    if(preview==0f) { super.dispatchDraw(canvas);return }
    val saved=canvas.save();val scale=1f-.12f*preview;canvas.scale(scale,scale,width/2f,height/2f);super.dispatchDraw(canvas);canvas.restoreToCount(saved)
  }
  override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) {
    super.onSizeChanged(w,h,oldw,oldh);animation?.cancel();preview=0f;policy.cancel();cancelled=true
  }
  override fun onDetachedFromWindow() { animation?.cancel();animation=null;captured=false;policy.cancel();super.onDetachedFromWindow() }
}

class KitapGestureHostManager: ViewGroupManager<KitapGestureHost>(),KitapGestureHostManagerInterface<KitapGestureHost> {
  private val delegate=KitapGestureHostManagerDelegate(this)
  override fun getDelegate(): ViewManagerDelegate<KitapGestureHost> =delegate
  override fun getName()="KitapGestureHost"
  override fun createViewInstance(context: ThemedReactContext)=KitapGestureHost(context)
  override fun setOverviewVisible(view: KitapGestureHost,value: Boolean) { view.overviewVisible(value) }
  override fun setOverviewEnabled(view: KitapGestureHost,value: Boolean) { view.overviewEnabled=value }
  override fun setReduceMotion(view: KitapGestureHost,value: Boolean) { view.motionReduced(value) }
}
