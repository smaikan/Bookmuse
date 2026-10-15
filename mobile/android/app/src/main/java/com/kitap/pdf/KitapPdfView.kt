package com.kitap.pdf

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.*
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.Event
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

class KitapPdfView(context: ThemedReactContext) : View(context), ComponentCallbacks2 {
  private val engine = PdfEngine.get(context)
  private val main = Handler(Looper.getMainLooper())
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val viewport = PageViewport()
  private var options = ReaderOptions()
  private var paper = Color.WHITE
  private var frame: PdfEngine.Frame? = null
  private var visible: Request? = null
  private val auxiliaryViewport=PageViewport()
  private var pendingDrag: Float?=null
  private var previousPageIndex=-1
  private var nextPageIndex=-1
  private var speculationBlocked=false
  private var departingOverlay: PdfEngine.Overlay?=null
  private var neighborOverlay: PdfEngine.Overlay?=null
  private var stagedOverlay: PdfEngine.Overlay?=null
  private var departing: PdfEngine.Frame? = null
  private var neighbor: Pair<Request, PdfEngine.Frame>? = null
  private val tiles = linkedMapOf<String, Pair<TileLayout.Tile, PdfEngine.Frame>>()
  private var sessionId = ""
  private var pageIndex = 0
  private var pageCount = 1
  private var loading = true
  private var unsettled = false
  private var slide = 0f
  private var animation: ValueAnimator? = null
  private var transitionDirection = 0
  private val generation = AtomicLong()
  private val tileGeneration = AtomicLong()
  private val requested = AtomicReference<Request?>(null)
  private val scheduled = AtomicBoolean()
  private val neighborRequested = AtomicReference<Request?>(null)
  private val neighborScheduled = AtomicBoolean()
  private var neighborTarget = -1
  private val tileScheduled = AtomicBoolean()
  private var acceptedTouch = false
  private var downX = 0f
  private var downY = 0f
  private var lastX = 0f
  private var lastY = 0f
  private var gestureNotified = false
  private var multiple = false
  private var velocity: VelocityTracker? = null
  private var lastZoomCommand = 0
  private var externalNavigation = false
  private var trackReading = true
  private var allowPrefetch = true
  private var pixelLimit = 1_500_000
  private var showHandle = false
  private var handleTouch = false
  private val handle = RectF()
  private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var deferPresentation = false
  private var presentationId = 0
  private var staged: Pair<Request,PdfEngine.Frame>? = null
  private val location = IntArray(2)
  private val rootLocation = IntArray(2)
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val tilesAfterSettle = Runnable { requestTiles() }
  private var metadata: TextGeometry?=null
  private var metadataPending=false
  private var textToken=0L
  @Volatile private var selectionToken=0L
  private var selectionId=""
  private var selectionStart=-1
  private var selectionEnd=-1
  private var selectionArmed=false
  private var selecting=false
  private var previewLease: ByteBudget.Lease?=null
  private var region: FloatArray?=null
  private var selectionQuads: List<FloatArray> = emptyList()
  private var pendingLongPress: Pair<Float,Float>?=null
  private var pendingAccessible=false
  private var handleDrag=0
  private val startHandle=RectF()
  private val endHandle=RectF()
  private val overlayPaint=Paint(Paint.ANTI_ALIAS_FLAG)
  private val overlayPath=Path()
  private var overlay: PdfEngine.Overlay?=null
  private var overlayToken=0L
  private var lastSelectionCommand=0
  private var lastAnnotationRevision=0
  private var focusCommand=""
  private var focusPending=false
  private val focusWhenSettled=Runnable { applyFocus() }

  private val taps = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(event: MotionEvent) = true
    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
      if(acceptedTouch && !multiple && selectionArmed) {
        selectionArmed=false;selecting=true;pendingLongPress=viewport.normalizedX(event.x) to viewport.normalizedY(event.y);selectPending()
      } else if (acceptedTouch && !multiple && !selecting && abs(slide) < touchSlop) emit("toggleTools", "", 0.0, 0.0)
      return true
    }
    override fun onDoubleTap(event: MotionEvent): Boolean {
      if (!loading && gestureHost()?.canZoomTogether()!=false) {
        clearSelection()
        stopTransition()
        viewport.zoomAt(if (viewport.zoom > 1.1f) 1f / viewport.zoom else 2f, event.x, event.y)
        zoomChanged(); requestTiles()
        gestureHost()?.zoomTogether(this@KitapPdfView,viewport.zoom,event.x/width,event.y/height)
      }
      return true
    }
    override fun onLongPress(event: MotionEvent) {
      if(!acceptedTouch || multiple || loading || visible?.page!=pageIndex || !viewport.contains(event.x,event.y) || handleTouch) return
      gestureHost()?.prepareSelection(this@KitapPdfView);dropSpeculation();slide=0f;stopTransition();releaseNeighbor();selecting=true;pendingLongPress=viewport.normalizedX(event.x) to viewport.normalizedY(event.y)
      if(!trackReading) emit("activatePage","",0.0,0.0) else selectPending()
    }
  })
  private val pinch = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
      if (loading) return false
      clearSelection()
      multiple = true; slide = 0f; stopTransition(); releaseNeighbor(); clearTiles()
      return true
    }
    override fun onScale(detector: ScaleGestureDetector): Boolean {
      viewport.zoomAt(detector.scaleFactor, detector.focusX, detector.focusY)
      invalidate(); return true
    }
    override fun onScaleEnd(detector: ScaleGestureDetector) { emit("zoom", "", 0.0, 0.0); settleTiles() }
  })

  init {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets -> invalidate(); insets }
    configurePaint()
  }

  fun setSession(value: String) {
    if (sessionId == value) return
    pendingDrag=null;generation.incrementAndGet(); sessionId = value; requested.set(null)
    releaseAll(); viewport.reset(); transitionDirection = 0; loading = true
    requestRender(); invalidate()
  }
  fun setPreviousPageIndex(value: Int) { previousPageIndex=value }
  fun setNextPageIndex(value: Int) { nextPageIndex=value }
  private fun gestureHost()=parent as? KitapGestureHost
  private fun holdParent(value: Boolean) { val host=gestureHost();if(host!=null) host.parent?.requestDisallowInterceptTouchEvent(value) else parent?.requestDisallowInterceptTouchEvent(value) }
  fun canCoordinateZoom()=!loading && frame!=null
  fun isReadingPane()=trackReading
  fun coordinatedZoom()=viewport.zoom
  fun beginCoordinatedZoom() { clearSelection();multiple=true;acceptedTouch=false;pendingDrag=null;stopTransition();dropSpeculation();clearTiles();emit("interaction","",0.0,0.0) }
  fun coordinateZoom(value: Float,x: Float,y: Float) { viewport.zoomAt(value/viewport.zoom,x,y);invalidate() }
  fun finishCoordinatedZoom() { speculationBlocked=false;zoomChanged() }
  fun openCoordinatedOverview() { emit("openPanel","",0.0,0.0) }
  fun coordinatePan(dx: Float,dy: Float) { if(!loading) { viewport.pan(dx,dy);clearTiles();invalidate();settleTiles() } }
  fun coordinateDrag(dx: Float,direction: Int) { if(!loading && !selecting && viewport.zoom<=1.01f) { slide=dx.coerceIn(-width*.8f,width*.8f);prepareNeighbor(direction);invalidate() } }
  fun finishCoordinatedDrag(commit: Boolean) { if(commit) pendingDrag=if(neighbor!=null) slide else null else { pendingDrag=null;bounce() } }
  fun dropSpeculation() { speculationBlocked=true;neighborRequested.set(null);releaseNeighbor() }
  fun setPageCount(value: Int) { pageCount = max(1, value) }
  fun setExternalNavigation(value: Boolean) { externalNavigation=value }
  fun setTrackReading(value: Boolean) { if(trackReading!=value) { trackReading=value;if(value && !loading) { unsettled=true;selectPending();applyFocus();invalidate() } else if(!value) { releaseText();clearSelection() } } }
  fun setAllowPrefetch(value: Boolean) { allowPrefetch=value;if(!value) { releaseNeighbor();neighborRequested.set(null) } }
  fun setPixelLimit(value: Int) { pixelLimit=value.coerceIn(100_000,1_500_000) }
  fun setShowHandle(value: Boolean) { showHandle=value;invalidate() }
  fun setDeferPresentation(value: Boolean) { deferPresentation=value }
  fun setPresentationId(value: Int) {
    if(value==presentationId) return
    presentationId=value
    val pending=staged ?: return;staged=null;val marks=stagedOverlay;stagedOverlay=null
    if(valid(pending.first)) acceptFrame(pending.first,pending.second,marks) else { pending.second.close();marks?.close() }
  }
  fun setPageIndex(value: Int) {
    if (value < 0 || (pageIndex == value && (loading || visible?.page == value))) return
    transitionDirection = if (visible != null) (value - visible!!.page).sign else 0
    pageIndex = value; speculationBlocked=false; generation.incrementAndGet(); stopTransition(); releaseNeighborExcept(value); clearTiles()
    releaseText();clearSelection();overlayToken++
    staged?.second?.close();staged=null;stagedOverlay?.close();stagedOverlay=null
    viewport.reset(); loading = true
    if (visible?.page == value && visible?.id == sessionId && frame != null) {
      visible = Request(sessionId, value, width, height, generation.get())
      unsettled = true; loading = false; slide = 0f; invalidate()
    } else requestRender()
  }
  fun setColorMode(value: Int) { options = options.copy(theme = if (value == 2) 3 else if (value == 1) 2 else 0); configurePaint(); invalidate() }
  fun setReaderOptions(value: String) {
    val parsed = try { ReaderOptions.parse(value) } catch (_: Exception) { emit("error", "Okuma ayarı geçersiz", 0.0, 0.0); return }
    if (parsed == options) return
    val geometryChanged = parsed.left != options.left || parsed.right != options.right || parsed.top != options.top ||
      parsed.bottom != options.bottom || parsed.fitWidth != options.fitWidth
    options = parsed; configurePaint()
    if (geometryChanged) {
      clearTiles(); releaseNeighbor()
      frame?.let { viewport.layout(width, height, it.bitmap.width, it.bitmap.height, options) }
      settleTiles()
    }
    invalidate()
  }
  fun setZoomCommand(value: Int) {
    if (value == 0 || value == lastZoomCommand) return
    lastZoomCommand = value
    if (loading || frame == null) return
    when (value % 10) {
      1 -> viewport.zoomAt(1.5f, width / 2f, height / 2f)
      2 -> viewport.zoomAt(1f / 1.5f, width / 2f, height / 2f)
      3 -> viewport.reset()
      4 -> viewport.pan(width*.25f,0f)
      5 -> viewport.pan(-width*.25f,0f)
      6 -> viewport.pan(0f,height*.25f)
      7 -> viewport.pan(0f,-height*.25f)
    }
    zoomChanged(); requestTiles()
  }
  fun setSelectionCommand(value: Int) {
    if(value==0 || value==lastSelectionCommand) return;lastSelectionCommand=value
    if(value%10!=3 && (loading || visible?.page!=pageIndex)) { emit("selectionError","Önce görüntülenen sayfayı yeniden açın.",0.0,0.0);return }
    when(value%10) {
      1 -> {
        val accessibility=context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if(accessibility.isTouchExplorationEnabled) { selecting=true;pendingAccessible=true;pendingLongPress=viewport.normalizedX(width/2f) to viewport.normalizedY(height/2f);selectPending() }
        else { selectionArmed=true;emit("selectionArmed","İşaretlemek istediğin kelimeye dokun veya uzun bas.",0.0,0.0) }
      }
      2 -> { createRegion(viewport.normalizedX(width/2f),viewport.normalizedY(height/2f)) }
      3 -> clearSelection()
      6,9 -> region?.let { r ->
        val dx=if(value%10==6) .025f else -.025f;val dy=if(value%10==6) .015f else -.015f
        val left=(r[0]-dx).coerceIn(options.left.toFloat(),r[2]-.005f);val right=(r[2]+dx).coerceIn(left+.005f,(1-options.right).toFloat())
        val top=(r[1]-dy).coerceIn(options.top.toFloat(),r[3]-.005f);val bottom=(r[3]+dy).coerceIn(top+.005f,(1-options.bottom).toFloat())
        r[0]=left;r[1]=top;r[2]=right;r[3]=bottom;updateRegionPreview();publishSelection()
      }
      4,5,7,8 -> metadata?.let { m ->
        if(selecting && region==null) {
          when(value%10) {
            4 -> selectionStart=m.wordRange((selectionStart-1).coerceAtLeast(0)).first
            5 -> selectionEnd=m.wordRange((selectionEnd+1).coerceAtMost(m.count-1)).last
            7 -> selectionStart=m.wordRange((selectionStart+1).coerceAtMost(selectionEnd)).first
            8 -> selectionEnd=m.wordRange((selectionEnd-1).coerceAtLeast(selectionStart)).last
          }
          if(updateSelectionPreview()) publishSelection()
        }
      }
    }
  }
  fun setAnnotationRevision(value: Int) { if(value!=lastAnnotationRevision) { lastAnnotationRevision=value;refreshOverlay() } }
  fun setFocusCommand(value: String) { if(value==focusCommand) return;focusCommand=value;focusPending=value.isNotEmpty();applyFocus() }
  private fun releaseText() { textToken++;metadata?.close();metadata=null;metadataPending=false }
  private fun clearOverlay() { overlayToken++;overlay?.close();overlay=null }
  private fun clearSelection() {
    multiple=true
    previewLease?.close();previewLease=null
    selectionArmed=false;pendingLongPress=null;pendingAccessible=false
    selectionToken++;val token=selectionId;selectionId="";selecting=false;region=null;selectionStart=-1;selectionEnd=-1;selectionQuads=emptyList();handleDrag=0;startHandle.setEmpty();endHandle.setEmpty()
    val id=sessionId
    if(token.isNotEmpty()) try { engine.execute { engine.clearSelection(id,token) } } catch(_: RejectedExecutionException) { }
    emit("selectionCleared","",0.0,0.0);invalidate()
  }
  private fun prepareText() {
    if(!trackReading || loading || visible?.page!=pageIndex || metadata!=null || metadataPending || !isAttachedToWindow || frame==null) return
    metadataPending=true;val token=++textToken;val id=sessionId;val page=pageIndex
    try { engine.execute(1) {
      val result=try { engine.prepareText(id,page) } catch(error: Exception) {
        main.post { if(token==textToken) { metadataPending=false;emit("selectionError",error.message ?: "Metin hazırlanamadı; bölge işaretle kullanılabilir.",0.0,0.0) } };null
      }
      if(result!=null) main.post {
        if(token!=textToken || id!=sessionId || page!=pageIndex || !trackReading || !isAttachedToWindow) result.close()
        else { metadata=result;metadataPending=false;selectPending() }
      }
    } } catch(_: RejectedExecutionException) { metadataPending=false;emit("selectionError","Metin motoru meşgul. Tekrar deneyin.",0.0,0.0) }
  }
  private fun selectPending() {
    val point=pendingLongPress ?: return
    val m=metadata ?: run { prepareText();emit("selectionLoading","Metin hazırlanıyor…",0.0,0.0);return }
    pendingLongPress=null
    if(m.count==0) { createRegion(point.first,point.second);return }
    var hit=m.hit(point.first,point.second)
    if(hit<0 && pendingAccessible) {
      hit=(0 until m.count).firstOrNull { i -> val q=m.quad(i,true);m.code(i)>32 && q[0] in viewport.normalizedX(0f)..viewport.normalizedX(width.toFloat()) && q[1] in viewport.normalizedY(0f)..viewport.normalizedY(height.toFloat()) } ?: -1
    }
    pendingAccessible=false
    if(hit<0) { clearSelection();emit("selectionError","Bu noktada metin yok. Bölge işaretle seçeneğini kullanabilirsiniz.",0.0,0.0);return }
    val word=m.wordRange(hit);selectionStart=word.first;selectionEnd=word.last;region=null;selecting=true
    if(updateSelectionPreview()) publishSelection()
  }
  private fun createRegion(x: Float,y: Float) {
    if(loading || visible?.page!=pageIndex || !trackReading) return
    if(previewLease==null) previewLease=engine.textBudget.reserve(131_072) ?: run { emit("selectionError","Seçim belleği meşgul",0.0,0.0);return }
    gestureHost()?.prepareSelection(this);dropSpeculation();releaseNeighbor();selecting=true;selectionStart=-1;selectionEnd=-1
    region=floatArrayOf((x-.12f).coerceIn(options.left.toFloat(),(1-options.right-.005).toFloat()),(y-.04f).coerceIn(options.top.toFloat(),(1-options.bottom-.005).toFloat()),(x+.12f).coerceIn((options.left+.005).toFloat(),(1-options.right).toFloat()),(y+.04f).coerceIn((options.top+.005).toFloat(),(1-options.bottom).toFloat()))
    updateRegionPreview();publishSelection()
  }
  private fun updateRegionPreview() { val r=region ?: return;selectionQuads=listOf(floatArrayOf(r[0],r[3],r[2],r[3],r[2],r[1],r[0],r[1]));invalidate() }
  private fun updateSelectionPreview(): Boolean {
    if(previewLease==null) previewLease=engine.textBudget.reserve(131_072) ?: run { emit("selectionError","Seçim belleği meşgul",0.0,0.0);return false }
    return try { selectionQuads=metadata?.ranges(selectionStart,selectionEnd,true).orEmpty();invalidate();true }
    catch(error: Exception) { clearSelection();emit("selectionError",error.message ?: "Bu pasaj seçilemedi",0.0,0.0);false }
  }
  private fun publishSelection() {
    val token=++selectionToken;val id=sessionId;val page=pageIndex;val marker=java.util.UUID.randomUUID().toString()
    val r=region?.copyOf();val a=selectionStart;val b=selectionEnd;val m=if(r==null) metadata?.retain() else null
    if(r==null && m==null) return
    selectionId=marker;emit("selectionLoading","Seçim hazırlanıyor…",0.0,0.0)
    try { engine.execute {
      try {
        if(token==selectionToken) {
          val result=if(r!=null) engine.selectRegion(id,page,r,marker) else engine.select(id,page,checkNotNull(m),a,b,marker)
          main.post { if(token==selectionToken && id==sessionId && page==pageIndex && isAttachedToWindow) emit("selection",result,0.0,0.0) }
        }
      } catch(error: Exception) { main.post { if(token==selectionToken) emit("selectionError",error.message ?: "Seçim hazırlanamadı",0.0,0.0) } }
      finally { m?.close() }
    } } catch(_: RejectedExecutionException) { m?.close();emit("selectionError","Motor meşgul. Seçimi yeniden deneyin.",0.0,0.0) }
  }
  private fun refreshOverlay() {
    if(loading || visible?.page!=pageIndex || sessionId.isEmpty() || frame==null || !isAttachedToWindow) return
    clearOverlay();val token=overlayToken;val id=sessionId;val page=pageIndex
    try { engine.execute(1) {
      try { val result=engine.overlays(id,page);main.post { if(token==overlayToken && id==sessionId && page==pageIndex && isAttachedToWindow) { overlay=result;invalidate() } else result.close() } }
      catch(error: Exception) { main.post { if(token==overlayToken) emit("annotationError",error.message ?: "İşaretler yüklenemedi",0.0,0.0) } }
    } } catch(_: RejectedExecutionException) { emit("annotationError","İşaretler için motor meşgul; sayfayı yeniden açın.",0.0,0.0) }
  }
  private fun applyFocus() {
    if(!focusPending || loading || !trackReading || frame==null || !isAttachedToWindow) return
    if(gestureHost()?.canZoomTogether()==false) { main.removeCallbacks(focusWhenSettled);main.postDelayed(focusWhenSettled,16);return }
    val payload=try { org.json.JSONObject(focusCommand) } catch(_: Exception) { return }
    if(payload.optInt("page",-1)!=pageIndex) return
    focusPending=false;val token=generation.get();val id=sessionId;val command=focusCommand
    try { engine.execute {
      try {
        val point=if(payload.has("mark")) engine.annotationFocus(id,payload.getString("mark"),payload.getInt("page")) else floatArrayOf(payload.getDouble("x").toFloat(),payload.getDouble("y").toFloat())
        main.post { if(token==generation.get() && id==sessionId && command==focusCommand && isAttachedToWindow) {
          if(gestureHost()?.canZoomTogether()==false) { focusPending=true;applyFocus();return@post }
          if(payload.has("mark") && (point[0] !in options.left.toFloat()..(1-options.right).toFloat() || point[1] !in options.top.toFloat()..(1-options.bottom).toFloat())) {
            emit("annotationOutsideCrop","İşaret kırpılan bölümde. Görmek için kırpmayı sıfırlayabilirsiniz.",0.0,0.0);return@post
          }
          viewport.zoomAt((payload.optDouble("zoom",1.5).toFloat()/viewport.zoom).coerceIn(.2f,5f),width/2f,height/2f)
          viewport.pan(width/2f-viewport.x(point[0]),height/2f-viewport.y(point[1]));zoomChanged()
          gestureHost()?.zoomTogether(this,viewport.zoom,.5f,.5f)
        } else if(id==sessionId && command==focusCommand && isAttachedToWindow) { focusPending=true;applyFocus() } }
      } catch(_: Exception) { main.post { if(id==sessionId && command==focusCommand) emit("annotationError","İşaret konumu açılamadı; sayfa ve alıntı korunuyor.",0.0,0.0) } }
    } } catch(_: RejectedExecutionException) { }
  }

  override fun onAttachedToWindow() { super.onAttachedToWindow(); context.applicationContext.registerComponentCallbacks(this); ViewCompat.requestApplyInsets(this); requestRender() }
  override fun onDetachedFromWindow() {
    generation.incrementAndGet(); requested.set(null); neighborRequested.set(null)
    main.removeCallbacks(tilesAfterSettle); acceptedTouch = false; velocity?.recycle(); velocity = null
    main.removeCallbacks(focusWhenSettled)
    context.applicationContext.unregisterComponentCallbacks(this);releaseAll(); super.onDetachedFromWindow()
  }
  override fun onTrimMemory(level: Int) { main.post { releaseNeighbor();neighborRequested.set(null);clearTiles();if(!selecting) releaseText();invalidate() } }
  override fun onLowMemory() { onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) }
  override fun onConfigurationChanged(configuration: Configuration) { super.onConfigurationChanged(configuration) }
  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    pendingDrag=null;generation.incrementAndGet(); stopTransition(); releaseNeighbor(); clearTiles()
    frame?.let { viewport.layout(w, h, it.bitmap.width, it.bitmap.height, options) }
    requestRender()
  }

  private fun requestRender() {
    if (sessionId.isEmpty() || width < 1 || height < 1 || !isAttachedToWindow) return
    val request = Request(sessionId, pageIndex, width, height, generation.get())
    loading = true
    val prepared = neighbor
    if (prepared != null && prepared.first.id == request.id && prepared.first.page == request.page &&
      prepared.first.width == width && prepared.first.height == height) {
      neighbor=null;val marks=neighborOverlay;neighborOverlay=null;neighborTarget=-1
      if(deferPresentation && visible?.page!=request.page) { staged?.second?.close();stagedOverlay?.close();staged=request to prepared.second;stagedOverlay=marks;emit("prepared","",prepared.second.renderMs,prepared.second.bitmap.allocationByteCount.toDouble(),request) }
      else acceptFrame(request,prepared.second,marks)
      return
    }
    requested.set(request); scheduleRender()
  }
  private fun scheduleRender() {
    if (!scheduled.compareAndSet(false, true)) return
    try { engine.execute {
      val request = requested.getAndSet(null)
      try {
        if (request != null) {
          val rendered = engine.render(request.id, request.page, request.width, request.height,pixelLimit)
          val marks=try { if(rendered!=null) engine.overlays(request.id,request.page) else null } catch(error: Exception) { rendered?.close();throw error }
          main.post {
            if (!valid(request)) { rendered?.close();marks?.close() }
            else if (rendered != null) {
              if(deferPresentation && visible?.page!=request.page) {
                staged?.second?.close();stagedOverlay?.close();stagedOverlay=marks;staged=request to rendered;emit("prepared","",rendered.renderMs,rendered.bitmap.allocationByteCount.toDouble(),request)
              } else acceptFrame(request, rendered,marks)
            }
          }
        }
      } catch (_: Exception) {
        main.post { if (request != null && valid(request)) { loading = false; bounce(); emit("error", "Sayfa görüntülenemedi. Önceki sayfa korundu.", 0.0, 0.0, request) } }
      } finally { scheduled.set(false); if (requested.get() != null) scheduleRender() }
    } } catch (_: RejectedExecutionException) {
      scheduled.set(false); requested.set(null); loading = false
      main.post { if (isAttachedToWindow) emit("error", "Motor meşgul. Sayfayı tekrar deneyin.", 0.0, 0.0) }
    }
  }
  private fun valid(request: Request) = isAttachedToWindow && request.id == sessionId && request.page == pageIndex && request.generation == generation.get()
  private fun acceptFrame(request: Request, rendered: PdfEngine.Frame,marks: PdfEngine.Overlay?=null) {
    stopTransition(); clearTiles(); releaseNeighbor()
    val oldMarks=overlay;overlay=marks;overlayToken++
    val drag=pendingDrag;pendingDrag=null
    val changedPage = visible?.page != request.page
    if (changedPage && frame != null && transitionDirection != 0 && !options.reduceMotion) {
      departingOverlay=oldMarks;departing = frame; frame = rendered; visible = request
      viewport.layout(width, height, rendered.bitmap.width, rendered.bitmap.height, options, preserve = false)
      val start = WipeTransition.start(width,transitionDirection,drag)
      animateSlide(start, 0f) { departing?.close();departingOverlay?.close();departingOverlay=null; departing = null; settleFrame() }
    } else {
      oldMarks?.close();frame?.close(); frame = rendered; visible = request
      viewport.layout(width, height, rendered.bitmap.width, rendered.bitmap.height, options, preserve = !changedPage)
      slide = 0f; settleFrame()
    }
  }
  private fun settleFrame() { loading = false; unsettled = true; transitionDirection = 0; invalidate(); settleTiles(); prepareNeighbor(1);selectPending();if(overlay==null) refreshOverlay();applyFocus() }
  private fun animateSlide(from: Float, to: Float, done: () -> Unit) {
    stopAnimationOnly(); slide = from
    if (options.reduceMotion) { slide = to; done(); return }
    animation = ValueAnimator.ofFloat(from, to).apply {
      duration = WipeTransition.duration(width,from,to); addUpdateListener { slide = it.animatedValue as Float; invalidate() }
      addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(animation: Animator) { this@KitapPdfView.animation = null; done() } })
      start()
    }
  }
  private fun stopAnimationOnly() { animation?.removeAllListeners(); animation?.cancel(); animation = null }
  private fun stopTransition() { stopAnimationOnly(); departing?.close();departingOverlay?.close();departingOverlay=null; departing = null; slide = 0f }
  private fun bounce() { animateSlide(slide, 0f) { invalidate() } }

  private fun prepareNeighbor(direction: Int) {
    if (selecting || selectionArmed || speculationBlocked || !allowPrefetch || loading || viewport.zoom > 1.01f || frame == null) return
    val target = if(externalNavigation) (if(direction>0) nextPageIndex else previousPageIndex) else (visible?.page ?: pageIndex) + direction
    if (target !in 0 until pageCount || neighbor?.first?.page == target || neighborTarget == target) return
    releaseNeighbor()
    neighborTarget = target
    neighborRequested.set(Request(sessionId, target, width, height, generation.get()))
    scheduleNeighbor()
  }
  private fun scheduleNeighbor() {
    if (!neighborScheduled.compareAndSet(false, true)) return
    try { engine.execute(2) {
      val request = neighborRequested.getAndSet(null)
      try {
        if (request != null) {
          val rendered = engine.render(request.id, request.page, request.width, request.height,pixelLimit)
          val marks=try { if(rendered!=null) engine.overlays(request.id,request.page) else null } catch(error: Exception) { rendered?.close();throw error }
          main.post {
            if (rendered != null) {
              if (!speculationBlocked && !selecting && !selectionArmed && request.id == sessionId && request.generation == generation.get() && request.page == neighborTarget && viewport.zoom <= 1.01f && isAttachedToWindow) {
                releaseNeighbor(); neighborTarget = request.page; neighbor = request to rendered;neighborOverlay=marks; invalidate()
              } else { rendered.close();marks?.close() }
            }
          }
        }
      } catch (_: Exception) { /* A failed speculative page never replaces the readable page. */ }
      finally { neighborScheduled.set(false); if (neighborRequested.get() != null) scheduleNeighbor() }
    } } catch (_: RejectedExecutionException) { neighborScheduled.set(false); neighborRequested.set(null) }
  }
  private fun releaseNeighbor() { neighborOverlay?.close();neighborOverlay=null;neighbor?.second?.close(); neighbor = null; neighborTarget = -1 }
  private fun releaseNeighborExcept(page: Int) { if (neighbor?.first?.page != page) releaseNeighbor() }
  private fun clearTiles() { tileGeneration.incrementAndGet(); tiles.values.forEach { it.second.close() }; tiles.clear() }
  private fun zoomChanged() { releaseNeighbor(); clearTiles(); invalidate(); emit("zoom", "", 0.0, 0.0); settleTiles() }
  private fun settleTiles() { main.removeCallbacks(tilesAfterSettle); main.postDelayed(tilesAfterSettle, 120) }
  private fun requestTiles() {
    val base = frame ?: return
    if (loading || viewport.pageWidth <= base.bitmap.width * 1.1f) { if (viewport.zoom <= 1.01f) prepareNeighbor(1); return }
    releaseNeighbor()
    val wanted = TileLayout.visible(viewport.pageWidth, viewport.pageHeight, viewport.offsetX, viewport.offsetY, width, height).take(16)
    val keys = wanted.map { it.key }.toSet()
    val obsolete = tiles.keys.filter { it !in keys }
    obsolete.forEach { tiles.remove(it)?.second?.close() }
    val next = wanted.firstOrNull { it.key !in tiles } ?: return
    if (!tileScheduled.compareAndSet(false, true)) return
    val request = visible ?: run { tileScheduled.set(false); return }
    val token = tileGeneration.get()
    try { engine.execute(1) {
      val rendered = try { engine.renderTile(request.id, request.page, next) } catch (_: Exception) { null }
      main.post {
        tileScheduled.set(false)
        if (rendered != null) {
          if (valid(request) && token == tileGeneration.get()) {
            tiles.put(next.key, next to rendered)?.second?.close(); invalidate(); requestTiles()
          } else { rendered.close(); requestTiles() }
        } else if (token != tileGeneration.get()) requestTiles()
      }
    } } catch (_: RejectedExecutionException) { tileScheduled.set(false) }
  }
  private fun releaseAll() { releaseText();clearSelection();clearOverlay();stopTransition(); releaseNeighbor(); clearTiles();staged?.second?.close();staged=null;stagedOverlay?.close();stagedOverlay=null; frame?.close(); frame = null; visible = null; unsettled = false }

  private fun interior(event: MotionEvent,index: Int=0): Boolean {
    val root = rootView
    val insets = ViewCompat.getRootWindowInsets(root) ?: return false
    val protected = insets.getInsets(WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.mandatorySystemGestures() or
      WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
    root.getLocationOnScreen(rootLocation)
    return GesturePolicy.inside(event.rawX-event.x+event.getX(index)-rootLocation[0], event.rawY-event.y+event.getY(index)-rootLocation[1], root.width, root.height,
      protected.left, protected.right, protected.top, protected.bottom)
  }
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
      acceptedTouch = interior(event)
      if (!acceptedTouch) return false
      if(animation!=null) return false
      speculationBlocked=false;pendingDrag=null
      downX = event.x; downY = event.y; lastX = downX; lastY = downY; multiple = false; gestureNotified=false
      handleTouch=showHandle && handle.contains(event.x,event.y)
      handleDrag=if(selecting && startHandle.contains(event.x,event.y)) 1 else if(selecting && endHandle.contains(event.x,event.y)) 2 else 0
      if(selecting) handleTouch=false
      velocity?.recycle(); velocity = VelocityTracker.obtain()
      holdParent(true)
    }
    if(event.actionMasked==MotionEvent.ACTION_POINTER_DOWN && (0 until event.pointerCount).any { !interior(event,it) }) { acceptedTouch=false;multiple=true;pendingDrag=null;bounce();holdParent(false);return false }
    if (!acceptedTouch) return false
    if(selecting && event.pointerCount==1) {
      if(event.actionMasked==MotionEvent.ACTION_MOVE && handleDrag!=0) {
        val x=viewport.normalizedX(event.x).coerceIn(options.left.toFloat(),(1-options.right).toFloat());val y=viewport.normalizedY(event.y).coerceIn(options.top.toFloat(),(1-options.bottom).toFloat())
        val r=region
        if(r!=null) { if(handleDrag==1) { r[0]=min(x,r[2]-.005f);r[1]=min(y,r[3]-.005f) } else { r[2]=max(x,r[0]+.005f);r[3]=max(y,r[1]+.005f) };updateRegionPreview() }
        else metadata?.let { m -> val hit=m.hit(x,y,.035f);if(hit>=0) { if(handleDrag==1) selectionStart=min(hit,selectionEnd) else selectionEnd=max(hit,selectionStart);updateSelectionPreview() } }
      }
      if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) {
        if(event.actionMasked==MotionEvent.ACTION_UP && handleDrag!=0) publishSelection()
        handleDrag=0;velocity?.recycle();velocity=null;holdParent(false)
      }
      return true
    }
    if(handleTouch) {
      if(event.actionMasked==MotionEvent.ACTION_UP || (event.actionMasked==MotionEvent.ACTION_MOVE && abs(event.x-downX)>24*resources.displayMetrics.density)) {
        handleTouch=false;acceptedTouch=false;velocity?.recycle();velocity=null;holdParent(false);emit("openPanel","",0.0,0.0)
      } else if(event.actionMasked==MotionEvent.ACTION_CANCEL) { handleTouch=false;acceptedTouch=false;holdParent(false) }
      return true
    }
    if(event.actionMasked==MotionEvent.ACTION_MOVE && !gestureNotified && (event.pointerCount>1 || abs(event.x-downX)>touchSlop || abs(event.y-downY)>touchSlop)) { gestureNotified=true;emit("interaction","",0.0,0.0) }
    velocity?.addMovement(event)
    if (event.pointerCount > 1) multiple = true
    pinch.onTouchEvent(event); taps.onTouchEvent(event)
    when (event.actionMasked) {
      MotionEvent.ACTION_MOVE -> if (!loading && !selectionArmed && !pinch.isInProgress && event.pointerCount == 1) {
        if (viewport.zoom > 1.01f || (options.fitWidth && abs(event.y - downY) > abs(event.x - downX))) {
          gestureHost()?.panPeers(this,event.x-lastX,event.y-lastY);viewport.pan(event.x - lastX, event.y - lastY); clearTiles(); invalidate(); settleTiles()
        } else if (!multiple && abs(event.x - downX) > abs(event.y - downY) * 1.2f && abs(event.x - downX) > touchSlop) {
          slide = (event.x - downX).coerceIn(-width * 0.8f, width * 0.8f)
          if ((slide > 0 && visible?.page == 0) || (slide < 0 && visible?.page == pageCount - 1)) slide *= 0.22f
          val direction=if(slide<0) 1 else -1;prepareNeighbor(direction);gestureHost()?.dragPeers(this,slide,direction);invalidate()
        }
        lastX = event.x; lastY = event.y
      }
      MotionEvent.ACTION_UP -> {
        if(trackReading && (viewport.zoom>1.01f || options.fitWidth)) emit("zoom","",0.0,0.0)
        velocity?.computeCurrentVelocity(1000)
        val delta = if (!loading && !selectionArmed && !multiple && viewport.zoom <= 1.01f) GesturePolicy.pageDelta(
          event.x - downX, event.y - downY, velocity?.xVelocity ?: 0f, width, resources.displayMetrics.density) else 0
        val origin = visible?.page ?: pageIndex
        if (delta != 0 && origin + delta in 0 until pageCount) {
          val target = origin + delta
          pendingDrag=if(neighbor?.first?.page==target) slide else null;gestureHost()?.endPeerDrag(this,true)
          emit("navigate", "Sayfa hazırlanıyor…", 0.0, 0.0, Request(sessionId, target, width, height, generation.get()),direction=delta)
          if(!externalNavigation) setPageIndex(target)
        } else { pendingDrag=null;gestureHost()?.endPeerDrag(this,false);if(abs(slide)>0f) bounce() }
        velocity?.recycle(); velocity = null; holdParent(false); settleTiles()
      }
      MotionEvent.ACTION_CANCEL -> {
        multiple = true;pendingDrag=null;gestureHost()?.endPeerDrag(this,false); velocity?.recycle(); velocity = null; acceptedTouch = false; bounce()
        holdParent(false)
      }
    }
    return true
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas);canvas.clipRect(0,0,width,height);canvas.drawColor(paper)
    val current = frame ?: return
    if (departing != null) {
      val oldViewport = auxiliaryViewport.apply { layout(width, height, departing!!.bitmap.width, departing!!.bitmap.height, options, false) }
      val dx=slide - if (slide >= 0) width else -width
      drawFrame(canvas, departing!!, oldViewport, dx);drawSavedMarks(canvas,oldViewport,departingOverlay,dx)
    } else if (slide != 0f) {
      neighbor?.let { (request, next) ->
        val other = auxiliaryViewport.apply { layout(width, height, next.bitmap.width, next.bitmap.height, options, false) }
        val dx=slide + if (request.page > pageIndex) width else -width
        drawFrame(canvas, next, other, dx);drawSavedMarks(canvas,other,neighborOverlay,dx)
      }
    }
    drawFrame(canvas, current, viewport, slide)
    if (slide == 0f) {
      val save = canvas.save()
      clipPage(canvas, viewport)
      for ((tile, rendered) in tiles.values) {
        val x = viewport.x(tile.x.toFloat() / tile.pageW); val y = viewport.y(tile.y.toFloat() / tile.pageH)
        canvas.drawBitmap(rendered.bitmap, null, RectF(x, y, viewport.x((tile.x + tile.width).toFloat() / tile.pageW),
          viewport.y((tile.y + tile.height).toFloat() / tile.pageH)), paint)
      }
      canvas.restoreToCount(save)
    }
    drawHandle(canvas)
    drawAnnotations(canvas,slide)
    val shown = visible
    if (unsettled && !loading && slide == 0f && shown != null && valid(shown)) {
      unsettled = false
      emit("ready", "Sayfa hazır", current.renderMs, current.bitmap.allocationByteCount.toDouble(), shown)
      if(!trackReading) return
      engine.recordPresentedPage(shown.id,shown.page)
      try { engine.execute {
        try {
          engine.saveDisplayedPage(shown.id, shown.page)
          main.post { if (shown.id == sessionId && isAttachedToWindow) emit("saved", "", 0.0, 0.0, shown) }
        } catch (_: Exception) { main.post { if (shown.id == sessionId && isAttachedToWindow) emit("saveError", "Konum kaydedilemedi. Tekrar deneyin.", 0.0, 0.0, shown) } }
      } } catch (_: RejectedExecutionException) { emit("saveError", "Konum kaydedilemedi. Tekrar deneyin.", 0.0, 0.0, shown) }
    }
  }
  private fun drawHandle(canvas: Canvas) {
    handle.setEmpty();if(!showHandle) return
    val root=rootView;val insets=ViewCompat.getRootWindowInsets(root) ?: return
    val protected=insets.getInsets(WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.mandatorySystemGestures() or WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
    root.getLocationOnScreen(rootLocation);getLocationOnScreen(location)
    val density=resources.displayMetrics.density;val size=48*density;val margin=8*density
    val minX=max(0f,(protected.left+rootLocation[0]-location[0]).toFloat())+margin
    val maxX=min(width.toFloat(),(root.width-protected.right+rootLocation[0]-location[0]).toFloat())-margin
    val minY=max(0f,(protected.top+rootLocation[1]-location[1]).toFloat())+margin
    val maxY=min(height.toFloat(),(root.height-protected.bottom+rootLocation[1]-location[1]).toFloat())-margin
    if(maxX-minX<size || maxY-minY<size) return
    val x=if(options.leftHand) minX else maxX-size
    val y=(height/2f-size/2).coerceIn(minY,maxY-size)
    handle.set(x,y,x+size,y+size)
    handlePaint.color=if(options.theme==3) Color.rgb(58,64,59) else Color.rgb(212,220,206)
    canvas.drawRoundRect(handle,8*density,8*density,handlePaint)
    handlePaint.color=if(options.theme==3) Color.rgb(218,219,215) else Color.rgb(54,75,55)
    handlePaint.strokeWidth=2*density
    for(row in -1..1) canvas.drawLine(x+15*density,y+size/2+row*6*density,x+33*density,y+size/2+row*6*density,handlePaint)
  }
  private fun drawSavedMarks(canvas: Canvas,transform: PageViewport,marks: PdfEngine.Overlay?,dx: Float) {
    if(marks==null) return
    val saved=canvas.save();canvas.translate(dx,0f);clipPage(canvas,transform)
    val normalized=canvas.save();canvas.translate(transform.offsetX,transform.offsetY);canvas.scale(transform.pageWidth,transform.pageHeight)
    overlayPaint.style=Paint.Style.FILL;overlayPaint.color=Color.rgb(255,235,59);overlayPaint.colorFilter=null;overlayPaint.alpha=82
    canvas.drawPath(marks.highlightPath,overlayPaint);canvas.restoreToCount(normalized)
    for(i in marks.colors.indices) if(marks.kinds[i]!=1) drawQuad(canvas,marks.quads,i*8,marks.colors[i],marks.kinds[i],transform)
    canvas.restoreToCount(saved)
  }
  private fun drawAnnotations(canvas: Canvas,dx: Float=0f) {
    drawSavedMarks(canvas,viewport,overlay,dx)
    val save=canvas.save();canvas.translate(dx,0f);clipPage(canvas,viewport)
    selectionQuads.forEach { drawQuad(canvas,it,0,Color.rgb(61,134,179),1) }
    canvas.restoreToCount(save)
    startHandle.setEmpty();endHandle.setEmpty()
    if(!selecting || selectionQuads.isEmpty()) return
    val first=selectionQuads.first();val last=selectionQuads.last()
    drawSelectionHandle(canvas,viewport.x(first[if(region!=null) 6 else 0]),viewport.y(first[if(region!=null) 7 else 1]),startHandle)
    drawSelectionHandle(canvas,viewport.x(last[2]),viewport.y(last[3]),endHandle)
  }
  private fun drawQuad(canvas: Canvas,q: FloatArray,offset: Int,color: Int,kind: Int,transform: PageViewport=viewport) {
    overlayPaint.color=color;overlayPaint.colorFilter=null;overlayPaint.strokeWidth=max(2f,2*resources.displayMetrics.density)
    overlayPaint.style=if(kind==1) Paint.Style.FILL else Paint.Style.STROKE
    overlayPaint.alpha=if(kind==1) 82 else 230
    if(kind==0) canvas.drawLine(transform.x(q[offset]),transform.y(q[offset+1]),transform.x(q[offset+2]),transform.y(q[offset+3]),overlayPaint)
    else {
      overlayPath.reset();overlayPath.moveTo(transform.x(q[offset]),transform.y(q[offset+1]))
      for(c in 1..3) overlayPath.lineTo(transform.x(q[offset+c*2]),transform.y(q[offset+c*2+1]))
      overlayPath.close();canvas.drawPath(overlayPath,overlayPaint)
    }
  }
  private fun drawSelectionHandle(canvas: Canvas,x: Float,y: Float,rect: RectF) {
    val root=rootView;val insets=ViewCompat.getRootWindowInsets(root) ?: return
    val p=insets.getInsets(WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.mandatorySystemGestures() or WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
    root.getLocationOnScreen(rootLocation);getLocationOnScreen(location)
    val radius=24*resources.displayMetrics.density
    val left=max(0f,(p.left+rootLocation[0]-location[0]).toFloat());val right=min(width.toFloat(),(root.width-p.right+rootLocation[0]-location[0]).toFloat())
    val top=max(0f,(p.top+rootLocation[1]-location[1]).toFloat());val bottom=min(height.toFloat(),(root.height-p.bottom+rootLocation[1]-location[1]).toFloat())
    if(right-left<radius*2 || bottom-top<radius*2) return
    val cx=x.coerceIn(left+radius,right-radius);val cy=y.coerceIn(top+radius,bottom-radius)
    rect.set(cx-radius,cy-radius,cx+radius,cy+radius)
    overlayPaint.style=Paint.Style.FILL;overlayPaint.color=Color.rgb(40,119,169);overlayPaint.alpha=255
    canvas.drawLine(x,y,cx,cy,overlayPaint);canvas.drawCircle(cx,cy,7*resources.displayMetrics.density,overlayPaint)
  }
  private fun clipPage(canvas: Canvas, transform: PageViewport) {
    canvas.clipRect(transform.x(options.left.toFloat()), transform.y(options.top.toFloat()),
      transform.x((1 - options.right).toFloat()), transform.y((1 - options.bottom).toFloat()))
  }
  private fun drawFrame(canvas: Canvas, rendered: PdfEngine.Frame, transform: PageViewport, slide: Float) {
    val save = canvas.save(); canvas.translate(slide, 0f); clipPage(canvas, transform)
    canvas.drawBitmap(rendered.bitmap, null, RectF(transform.offsetX, transform.offsetY,
      transform.offsetX + transform.pageWidth, transform.offsetY + transform.pageHeight), paint)
    canvas.restoreToCount(save)
  }
  private fun configurePaint() {
    paper = when (options.theme) { 1 -> Color.rgb(255, 245, 227); 2 -> Color.rgb(244, 231, 206); 3 -> Color.rgb(30, 33, 36); 4 -> Color.parseColor(options.paper); else -> Color.WHITE }
    val ink = when (options.theme) { 1 -> Color.rgb(48, 55, 45); 2 -> Color.rgb(63, 46, 32); 3 -> Color.rgb(218, 219, 215); 4 -> Color.parseColor(options.ink); else -> Color.BLACK }
    val contrast = options.contrast.toFloat()
    val matrix = if (options.theme == 0) ColorMatrix(floatArrayOf(contrast,0f,0f,0f,128*(1-contrast), 0f,contrast,0f,0f,128*(1-contrast),
      0f,0f,contrast,0f,128*(1-contrast), 0f,0f,0f,1f,0f)) else {
      val coefficients = floatArrayOf(0.2126f,0.7152f,0.0722f); val values = FloatArray(20)
      val light = intArrayOf(Color.red(paper),Color.green(paper),Color.blue(paper)); val dark = intArrayOf(Color.red(ink),Color.green(ink),Color.blue(ink))
      for (row in 0..2) { val delta=(light[row]-dark[row])/255f; for(column in 0..2) values[row*5+column]=delta*coefficients[column]*contrast
        values[row*5+4]=dark[row]+(light[row]-dark[row])*(1-contrast)/2f }
      values[18]=1f; ColorMatrix(values)
    }
    paint.colorFilter = if (options.theme == 0 && contrast == 1f) null else ColorMatrixColorFilter(matrix)
  }
  private fun emit(status: String, message: String, renderMs: Double, bitmapBytes: Double, request: Request? = null, direction: Int = 0) {
    if (id == NO_ID) return
    val reactContext = context as ReactContext
    val dispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id) ?: return
    val data = Arguments.createMap().apply {
      putString("status", status); putString("message", message); putDouble("renderMs", renderMs); putDouble("bitmapBytes", bitmapBytes)
      putString("sessionId", request?.id ?: sessionId); putInt("pageIndex", request?.page ?: pageIndex)
      putDouble("zoom", viewport.zoom.toDouble())
      putInt("direction",direction)
      putDouble("focusX",viewport.normalizedX(width/2f).toDouble());putDouble("focusY",viewport.normalizedY(height/2f).toDouble())
    }
    dispatcher.dispatchEvent(DiagnosticEvent(UIManagerHelper.getSurfaceId(reactContext), id, data))
  }
  private data class Request(val id: String, val page: Int, val width: Int, val height: Int, val generation: Long)
  private class DiagnosticEvent(surfaceId: Int, tag: Int, private val data: WritableMap) : Event<DiagnosticEvent>(surfaceId, tag) {
    override fun getEventName() = "topDiagnostic"
    // Ready/prepared and durable-save ACK are distinct state transitions. Fabric's
    // default coalescing can replace ready with saved before the JS frame consumes it.
    override fun canCoalesce() = false
    override fun getEventData() = data
  }
}
