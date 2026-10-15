package com.kitap.pdf

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

data class RenderSize(val width: Int, val height: Int) {
  val bytes: Long get() = width.toLong() * height.toLong() * 4L
  companion object {
    fun fit(pageWidth: Int, pageHeight: Int, viewWidth: Int, viewHeight: Int, maxPixels: Long = 1_500_000L): RenderSize {
      require(pageWidth > 0 && pageHeight > 0 && viewWidth > 0 && viewHeight > 0 && maxPixels > 0)
      val fitScale = min(viewWidth.toDouble() / pageWidth, viewHeight.toDouble() / pageHeight)
      val pixelScale = sqrt(maxPixels.toDouble() / (pageWidth.toDouble() * pageHeight))
      val scale = min(fitScale, pixelScale)
      return RenderSize(maxOf(1, floor(pageWidth * scale).toInt()), maxOf(1, floor(pageHeight * scale).toInt()))
    }
  }
}
