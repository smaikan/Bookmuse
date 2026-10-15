package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class GesturePolicyTest {
  @Test fun bothOuterEdgesAndSystemBarsAreProtected() {
    assertFalse(GesturePolicy.inside(12f, 500f, 1200, 800, 32, 40, 24, 48))
    assertFalse(GesturePolicy.inside(1170f, 500f, 1200, 800, 32, 40, 24, 48))
    assertFalse(GesturePolicy.inside(600f, 760f, 1200, 800, 32, 40, 24, 48))
    assertTrue(GesturePolicy.inside(600f, 500f, 1200, 800, 32, 40, 24, 48))
  }
  @Test fun smallAndDiagonalDragsCannotTurnPages() {
    assertEquals(0, GesturePolicy.pageDelta(150f, 10f, 0f, 1200, 1f))
    assertEquals(0, GesturePolicy.pageDelta(-400f, 500f, 1500f, 1200, 1f))
  }
  @Test fun committedDragAndFlingHaveCorrectDirection() {
    assertEquals(1, GesturePolicy.pageDelta(-400f, 10f, 0f, 1200, 1f))
    assertEquals(-1, GesturePolicy.pageDelta(40f, 5f, 800f, 1200, 1f))
    assertEquals(0, GesturePolicy.pageDelta(20f, 0f, 1200f, 1200, 1f))
    assertEquals(0, GesturePolicy.pageDelta(-40f, 0f, 1200f, 1200, 1f))
  }
}
