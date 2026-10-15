package com.kitap.pdf

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager

class KitapPdfPackage : BaseReactPackage() {
  override fun getModule(name: String, context: ReactApplicationContext): NativeModule? =
    if (name == KitapPdfModule.NAME) KitapPdfModule(context) else null
  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(KitapPdfModule.NAME to ReactModuleInfo(KitapPdfModule.NAME, KitapPdfModule::class.java.name, false, false, false, true))
  }
  override fun createViewManagers(context: ReactApplicationContext): List<ViewManager<*, *>> = listOf(KitapGestureHostManager(), KitapPdfViewManager(), KitapSliderManager(), KitapThumbnailManager())
}
