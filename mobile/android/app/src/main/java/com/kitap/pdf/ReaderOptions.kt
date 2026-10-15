package com.kitap.pdf

import org.json.JSONObject

data class ReaderOptions(
  val theme: Int = 0, val paper: String = "#F3EBDD", val ink: String = "#313B32",
  val contrast: Double = 1.0, val left: Double = 0.0, val top: Double = 0.0,
  val right: Double = 0.0, val bottom: Double = 0.0, val fitWidth: Boolean = false,
  val brightness: Double = -1.0, val orientation: String = "auto", val screenOn: Boolean = false,
  val fullscreen: Boolean = false, val counterAlways: Boolean = true, val reduceMotion: Boolean = false,
  val leftHand: Boolean = false, val doublePage: Boolean = false, val coverSingle: Boolean = true,
) {
  fun json() = JSONObject().put("theme", theme).put("paper", paper).put("ink", ink).put("contrast", contrast)
    .put("left", left).put("top", top).put("right", right).put("bottom", bottom).put("fitWidth", fitWidth)
    .put("brightness", brightness).put("orientation", orientation).put("screenOn", screenOn)
    .put("fullscreen", fullscreen).put("counterAlways", counterAlways).put("reduceMotion", reduceMotion)
    .put("leftHand", leftHand).put("doublePage", doublePage).put("coverSingle", coverSingle)
  companion object {
    private fun number(obj: JSONObject, name: String, fallback: Double, low: Double, high: Double): Double {
      val value = obj.optDouble(name, fallback)
      require(value.isFinite() && value in low..high) { "$name ayarı geçersiz" }
      return value
    }
    fun parse(value: String): ReaderOptions {
      if (value.isBlank()) return ReaderOptions()
      val obj = JSONObject(value)
      val theme = obj.optInt("theme", 0); require(theme in 0..4)
      val paper = obj.optString("paper", "#F3EBDD"); val ink = obj.optString("ink", "#313B32")
      require(Regex("^#[0-9a-fA-F]{6}$").matches(paper) && Regex("^#[0-9a-fA-F]{6}$").matches(ink)) { "Renk #RRGGBB biçiminde olmalı" }
      val orientation = obj.optString("orientation", "auto"); require(orientation in setOf("auto", "portrait", "landscape"))
      val brightness = number(obj, "brightness", -1.0, -1.0, 1.0)
      require(brightness == -1.0 || brightness >= 0.05) { "Parlaklık sistem değeri veya %5–100 olmalı" }
      return ReaderOptions(theme, paper, ink, number(obj, "contrast", 1.0, 0.6, 1.8),
        number(obj, "left", 0.0, 0.0, 0.3), number(obj, "top", 0.0, 0.0, 0.3),
        number(obj, "right", 0.0, 0.0, 0.3), number(obj, "bottom", 0.0, 0.0, 0.3),
        obj.optBoolean("fitWidth", false), brightness, orientation, obj.optBoolean("screenOn", false),
        obj.optBoolean("fullscreen", false), obj.optBoolean("counterAlways", true), obj.optBoolean("reduceMotion", false),
        obj.optBoolean("leftHand", false), obj.optBoolean("doublePage", false), obj.optBoolean("coverSingle", true))
    }
  }
}
