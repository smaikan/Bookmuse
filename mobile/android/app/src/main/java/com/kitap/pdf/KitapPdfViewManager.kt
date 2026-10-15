package com.kitap.pdf

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.KitapPdfViewManagerDelegate
import com.facebook.react.viewmanagers.KitapPdfViewManagerInterface

class KitapPdfViewManager : SimpleViewManager<KitapPdfView>(), KitapPdfViewManagerInterface<KitapPdfView> {
  private val delegate = KitapPdfViewManagerDelegate(this)
  override fun getDelegate(): ViewManagerDelegate<KitapPdfView> = delegate
  override fun getName() = "KitapPdfView"
  override fun createViewInstance(context: ThemedReactContext) = KitapPdfView(context)
  override fun setSessionId(view: KitapPdfView, value: String?) = view.setSession(value.orEmpty())
  override fun setPageIndex(view: KitapPdfView, value: Int) = view.setPageIndex(value)
  override fun setPreviousPageIndex(view: KitapPdfView,value:Int)=view.setPreviousPageIndex(value)
  override fun setNextPageIndex(view: KitapPdfView,value:Int)=view.setNextPageIndex(value)
  override fun setPageCount(view: KitapPdfView, value: Int) = view.setPageCount(value)
  override fun setReaderOptions(view: KitapPdfView, value: String?) = view.setReaderOptions(value.orEmpty())
  override fun setZoomCommand(view: KitapPdfView, value: Int) = view.setZoomCommand(value)
  override fun setExternalNavigation(view: KitapPdfView,value:Boolean)=view.setExternalNavigation(value)
  override fun setTrackReading(view: KitapPdfView,value:Boolean)=view.setTrackReading(value)
  override fun setAllowPrefetch(view: KitapPdfView,value:Boolean)=view.setAllowPrefetch(value)
  override fun setPixelLimit(view: KitapPdfView,value:Int)=view.setPixelLimit(value)
  override fun setShowHandle(view: KitapPdfView,value:Boolean)=view.setShowHandle(value)
  override fun setDeferPresentation(view: KitapPdfView,value:Boolean)=view.setDeferPresentation(value)
  override fun setPresentationId(view: KitapPdfView,value:Int)=view.setPresentationId(value)
  override fun setSelectionCommand(view: KitapPdfView,value:Int)=view.setSelectionCommand(value)
  override fun setAnnotationRevision(view: KitapPdfView,value:Int)=view.setAnnotationRevision(value)
  override fun setFocusCommand(view: KitapPdfView,value:String?)=view.setFocusCommand(value.orEmpty())
  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
    mutableMapOf("topDiagnostic" to mapOf("registrationName" to "onDiagnostic"))
}
