package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class SpreadTest {
  @Test fun coverOddCountAndSecondPageKeepPhysicalIdentity() {
    assertEquals(listOf(0),Spread.pages(0,6,true))
    assertEquals(listOf(1,2),Spread.pages(2,6,true))
    assertEquals(listOf(5),Spread.pages(5,6,true))
    assertEquals(listOf(4,5),Spread.pages(5,6,false))
  }
  @Test fun spreadNavigationDoesNotSkipOrWrapAtBookEnds() {
    assertEquals(1,Spread.next(0,6,true,1))
    assertEquals(3,Spread.next(2,6,true,1))
    assertEquals(1,Spread.next(4,6,true,-1))
    assertEquals(0,Spread.next(1,6,true,-1))
  }
}
