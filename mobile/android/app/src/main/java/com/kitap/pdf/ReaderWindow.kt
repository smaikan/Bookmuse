package com.kitap.pdf

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** UI-thread window effects with a single saved baseline; never change global display settings. */
class ReaderWindow {
  private var baseline: Baseline? = null
  fun apply(activity: Activity, options: ReaderOptions) {
    if (baseline?.activity !== activity) { restore(); baseline = Baseline(activity) }
    val window = activity.window
    window.attributes = window.attributes.apply { screenBrightness = options.brightness.toFloat() }
    activity.requestedOrientation = when (options.orientation) {
      "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      else -> baseline!!.orientation
    }
    if (options.screenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else if (!baseline!!.screenOn) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (options.fullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
    else { restoreBars(baseline!!) }
  }
  fun restore() {
    val saved = baseline ?: return
    baseline = null
    if (saved.activity.isDestroyed) return
    saved.activity.window.attributes = saved.activity.window.attributes.apply { screenBrightness = saved.brightness }
    saved.activity.requestedOrientation = saved.orientation
    if (saved.screenOn) saved.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else saved.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    restoreBars(saved)
  }
  private fun restoreBars(saved: Baseline) {
    val controller = WindowCompat.getInsetsController(saved.activity.window, saved.activity.window.decorView)
    for ((type, visible) in listOf(WindowInsetsCompat.Type.statusBars() to saved.statusVisible,
      WindowInsetsCompat.Type.navigationBars() to saved.navigationVisible)) {
      if (visible) controller.show(type) else controller.hide(type)
    }
  }
  private class Baseline(val activity: Activity) {
    val brightness = activity.window.attributes.screenBrightness
    val orientation = activity.requestedOrientation
    val screenOn = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
    private val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
    val statusVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
    val navigationVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
  }
}
