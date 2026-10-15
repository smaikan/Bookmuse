package com.kitap.pdf

import kotlin.math.max
import kotlin.math.min

/** Display-normalized full-page coordinates; PDFium handles PDF box origins and intrinsic rotation. */
class PageViewport {
  var zoom = 1f; private set
  var panX = 0f; private set
  var panY = 0f; private set
  var offsetX = 0f; private set
  var offsetY = 0f; private set
  var pageWidth = 1f; private set
  var pageHeight = 1f; private set
  private var width = 1f
  private var height = 1f
  private var crop = ReaderOptions()
  private var baseW = 1f
  private var baseH = 1f

  fun layout(w: Int, h: Int, bitmapW: Int, bitmapH: Int, options: ReaderOptions, preserve: Boolean = true) {
    val focusX = normalizedX(width / 2f); val focusY = normalizedY(height / 2f)
    width = max(1, w).toFloat(); height = max(1, h).toFloat(); crop = options
    val croppedW = bitmapW * (1.0 - options.left - options.right)
    val croppedH = bitmapH * (1.0 - options.top - options.bottom)
    val scale = if (options.fitWidth) width / croppedW else min(width / croppedW, height / croppedH)
    baseW = (bitmapW * scale).toFloat(); baseH = (bitmapH * scale).toFloat()
    if (!preserve) { zoom = 1f; panX = 0f; panY = 0f }
    update()
    if (preserve) { panX += width / 2f - x(focusX); panY += height / 2f - y(focusY); update() }
  }
  fun reset() { zoom = 1f; panX = 0f; panY = 0f; update() }
  fun zoomAt(factor: Float, x: Float, y: Float) {
    val anchorX = normalizedX(x); val anchorY = normalizedY(y)
    zoom = (zoom * factor).coerceIn(1f, 5f)
    update()
    panX += x - this.x(anchorX); panY += y - this.y(anchorY); update()
  }
  fun pan(dx: Float, dy: Float) { panX += dx; panY += dy; update() }
  fun normalizedX(x: Float) = (x - offsetX) / pageWidth
  fun normalizedY(y: Float) = (y - offsetY) / pageHeight
  fun x(normalized: Float) = offsetX + normalized * pageWidth
  fun y(normalized: Float) = offsetY + normalized * pageHeight
  fun contains(x: Float, y: Float): Boolean = normalizedX(x) in crop.left.toFloat()..(1 - crop.right).toFloat() &&
    normalizedY(y) in crop.top.toFloat()..(1 - crop.bottom).toFloat()
  private fun update() {
    pageWidth = baseW * zoom; pageHeight = baseH * zoom
    val visibleW = pageWidth * (1 - crop.left - crop.right).toFloat()
    val visibleH = pageHeight * (1 - crop.top - crop.bottom).toFloat()
    val maxX = max(0f, (visibleW - width) / 2f); val maxY = max(0f, (visibleH - height) / 2f)
    panX = panX.coerceIn(-maxX, maxX); panY = panY.coerceIn(-maxY, maxY)
    offsetX = (width - visibleW) / 2f + panX - pageWidth * crop.left.toFloat()
    offsetY = (height - visibleH) / 2f + panY - pageHeight * crop.top.toFloat()
  }
}
