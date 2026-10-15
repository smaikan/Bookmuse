package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class TileLayoutTest {
  @Test fun fiveTimesZoomAllocatesOnlySmallVisibleTiles() {
    val tiles = TileLayout.visible(6000f, 9000f, -2400f, -3500f, 1200, 1800)
    assertTrue(tiles.isNotEmpty()); assertTrue(tiles.size < 30)
    assertTrue(tiles.all { it.width in 1..512 && it.height in 1..512 })
    assertTrue(tiles.all { it.x > 1000 && it.y > 2000 })
  }
  @Test fun outOfPageViewportDoesNotRequestAnEntireDocument() {
    assertTrue(TileLayout.visible(1200f, 1800f, 5000f, 5000f, 1200, 800).isEmpty())
  }
}
