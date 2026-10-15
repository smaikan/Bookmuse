package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class PageViewportTest {
  @Test fun cropZoomPanRoundTripUsesFullPageNormalizedCoordinates() {
    val viewport = PageViewport()
    viewport.layout(1200, 800, 600, 900, ReaderOptions(left=.1, top=.05, right=.1, bottom=.08), false)
    viewport.zoomAt(3f, 600f, 400f); viewport.pan(80f, -120f)
    assertEquals(.4f, viewport.normalizedX(viewport.x(.4f)), .00001f)
    assertEquals(.6f, viewport.normalizedY(viewport.y(.6f)), .00001f)
    assertFalse(viewport.contains(viewport.x(.03f), viewport.y(.5f)))
  }
  @Test fun rotationKeepsTheNormalizedReadingFocus() {
    val viewport = PageViewport()
    viewport.layout(800, 1200, 600, 900, ReaderOptions(), false)
    viewport.zoomAt(3f, 400f, 600f); viewport.pan(70f, -120f)
    val x = viewport.normalizedX(400f); val y = viewport.normalizedY(600f)
    viewport.layout(1200, 800, 600, 900, ReaderOptions())
    assertEquals(x, viewport.normalizedX(600f), .0001f)
    assertEquals(y, viewport.normalizedY(400f), .0001f)
  }
  @Test fun zoomIsBoundedAndResetRestoresFit() {
    val viewport = PageViewport()
    viewport.layout(800, 1200, 600, 900, ReaderOptions(), false)
    viewport.zoomAt(100f, 400f, 600f); assertEquals(5f, viewport.zoom, 0f)
    viewport.zoomAt(.001f, 400f, 600f); assertEquals(1f, viewport.zoom, 0f)
    viewport.reset(); assertEquals(0f, viewport.panX, 0f); assertEquals(0f, viewport.panY, 0f)
  }
}
