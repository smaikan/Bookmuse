package com.kitap.pdf

import kotlin.math.*

object TileLayout {
  data class Tile(val x: Int, val y: Int, val width: Int, val height: Int, val pageW: Int, val pageH: Int) {
    val key get() = "$pageW:$pageH:$x:$y"
  }
  fun visible(pageW: Float, pageH: Float, offsetX: Float, offsetY: Float, viewW: Int, viewH: Int): List<Tile> {
    require(pageW > 0 && pageH > 0 && pageW.isFinite() && pageH.isFinite())
    val quality = min(1.0, sqrt(3_000_000.0 / max(1L, viewW.toLong() * viewH)))
    val w = max(1, ceil(pageW * quality).toInt()); val h = max(1, ceil(pageH * quality).toInt())
    val left = floor(-offsetX * quality).toInt().coerceIn(0, w)
    val top = floor(-offsetY * quality).toInt().coerceIn(0, h)
    val right = ceil((viewW - offsetX) * quality).toInt().coerceIn(0, w)
    val bottom = ceil((viewH - offsetY) * quality).toInt().coerceIn(0, h)
    val result = mutableListOf<Tile>()
    for (y in (top / 512 * 512) until bottom step 512) for (x in (left / 512 * 512) until right step 512) {
      result.add(Tile(x, y, min(512, w - x), min(512, h - y), w, h))
    }
    return result
  }
}
