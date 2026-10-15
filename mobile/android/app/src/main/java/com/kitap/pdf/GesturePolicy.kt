package com.kitap.pdf

import kotlin.math.abs

object GesturePolicy {
  fun inside(x: Float, y: Float, width: Int, height: Int, left: Int, right: Int, top: Int, bottom: Int): Boolean =
    x >= left && x < width - right && y >= top && y < height - bottom

  fun pageDelta(dx: Float, dy: Float, velocity: Float, width: Int, density: Float): Int {
    if (width <= 0 || abs(dx) <= abs(dy) * 1.2f) return 0
    val committed = abs(dx) >= width * 0.22f || (abs(dx) >= 32f * density && abs(velocity) >= 650f * density && velocity * dx > 0f)
    return if (!committed) 0 else if (dx < 0) 1 else -1
  }
}
