package com.kitap.pdf

import android.os.Bundle
import android.view.KeyEvent
import android.widget.SeekBar
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.events.Event
import com.facebook.react.viewmanagers.KitapSliderManagerDelegate
import com.facebook.react.viewmanagers.KitapSliderManagerInterface

class KitapSliderView(private val reactContext: ThemedReactContext) : SeekBar(reactContext) {
  var minimumValue = 0
  var maximumValue = 100
  var requestedValue = 0
  init {
    setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) { if (fromUser) emit("topValueChange") }
      override fun onStartTrackingTouch(view: SeekBar) { }
      override fun onStopTrackingTouch(view: SeekBar) { emit("topSlidingComplete") }
    })
  }
  fun configure() { max = (maximumValue - minimumValue).coerceAtLeast(0); progress = (requestedValue - minimumValue).coerceIn(0, max) }
  override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
    val handled = super.performAccessibilityAction(action, arguments)
    if (handled) emit("topSlidingComplete")
    return handled
  }
  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    val handled = super.onKeyUp(keyCode, event)
    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) emit("topSlidingComplete")
    return handled
  }
  private fun emit(name: String) {
    if (id == NO_ID) return
    val data = Arguments.createMap().apply { putInt("value", progress + minimumValue) }
    UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)?.dispatchEvent(SliderEvent(UIManagerHelper.getSurfaceId(reactContext), id, name, data))
  }
  private class SliderEvent(surface: Int, tag: Int, private val name: String, private val data: WritableMap) : Event<SliderEvent>(surface, tag) {
    override fun getEventName() = name
    override fun getEventData() = data
  }
}
class KitapSliderManager : SimpleViewManager<KitapSliderView>(), KitapSliderManagerInterface<KitapSliderView> {
  private val delegate = KitapSliderManagerDelegate(this)
  override fun getDelegate(): ViewManagerDelegate<KitapSliderView> = delegate
  override fun getName() = "KitapSlider"
  override fun createViewInstance(context: ThemedReactContext) = KitapSliderView(context)
  override fun setValue(view: KitapSliderView, value: Int) { view.requestedValue = value; view.configure() }
  override fun setMinimum(view: KitapSliderView, value: Int) { view.minimumValue = value; view.configure() }
  override fun setMaximum(view: KitapSliderView, value: Int) { view.maximumValue = value; view.configure() }
  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> = mutableMapOf(
    "topValueChange" to mapOf("registrationName" to "onValueChange"), "topSlidingComplete" to mapOf("registrationName" to "onSlidingComplete"))
}
