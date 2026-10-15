package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class RenderSizeTest {
  @Test fun hugeTabletOutputIsCappedWithoutLosingAspectRatio() {
    val size = RenderSize.fit(595, 842, 8000, 8000)
    assertTrue(size.width.toLong() * size.height <= 1_500_000)
    assertTrue(kotlin.math.abs(size.width.toDouble() / size.height - 595.0 / 842.0) < 0.002)
    assertTrue(size.bytes <= 6_000_000)
  }
  @Test fun fitsLandscapeAndSmallViewports() {
    for ((width, height) in listOf(100 to 400, 1000 to 300, 1 to 1)) {
      val size = RenderSize.fit(595, 842, width, height)
      assertTrue(size.width in 1..width)
      assertTrue(size.height in 1..height)
    }
  }
  @Test(expected = IllegalArgumentException::class)
  fun rejectsUnmeasuredViewport() { RenderSize.fit(595, 842, 0, 200) }
}
