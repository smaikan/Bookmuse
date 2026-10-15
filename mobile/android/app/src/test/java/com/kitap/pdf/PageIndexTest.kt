package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class PageIndexTest {
  @Test fun validPhysicalPagesAreZeroBased() {
    assertEquals(0, PageIndex.requireValid(0, 1))
    assertEquals(249, PageIndex.requireValid(249, 500))
    assertEquals(499, PageIndex.requireValid(499, 500))
  }
  @Test fun invalidNumbersCannotReachPdfium() {
    assertThrows(IllegalArgumentException::class.java) { PageIndex.requireValid(-1, 500) }
    assertThrows(IllegalArgumentException::class.java) { PageIndex.requireValid(500, 500) }
    assertThrows(IllegalArgumentException::class.java) { PageIndex.requireValid(0, 0) }
  }
}
