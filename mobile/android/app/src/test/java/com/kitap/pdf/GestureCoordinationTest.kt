package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class GestureCoordinationTest {
  @Test fun onlyReachingFitDoesNotOpenOverviewAndContinuedPinchArms() {
    val pinch=OverviewPinch();pinch.begin(2f);pinch.scale(.5f)
    assertEquals(1f,pinch.displayZoom,0f);assertFalse(pinch.armed)
    pinch.scale(.83f);assertFalse(pinch.armed);pinch.scale(.98f);assertTrue(pinch.armed)
    assertEquals(1f,pinch.displayZoom,0f)
  }
  @Test fun hysteresisRejectsShortPinchAndOpeningFingersCancelsArmedState() {
    val pinch=OverviewPinch();pinch.begin(1f);pinch.scale(.85f);assertFalse(pinch.armed)
    pinch.scale(.9f);assertTrue(pinch.armed);pinch.scale(1.1f);assertTrue(pinch.armed)
    pinch.scale(1.1f);assertFalse(pinch.armed);pinch.cancel();assertFalse(pinch.armed);assertEquals(1f,pinch.zoom,0f)
  }
  @Test fun invalidFactorsDoNotCorruptBoundsAndFiveIsTheDisplayLimit() {
    val pinch=OverviewPinch();pinch.begin(1f)
    for(factor in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f,0f))pinch.scale(factor)
    assertEquals(1f,pinch.zoom,0f);pinch.scale(100f);assertEquals(5f,pinch.displayZoom,0f)
    pinch.scale(.001f);assertEquals(1f,pinch.displayZoom,0f);assertTrue(pinch.armed)
  }
  @Test fun committedWipeContinuesFromTheSamePixelInBothDirections() {
    val next=WipeTransition.start(1000,1,-760f);assertEquals(240f,next,0f);assertEquals(-760f,next-1000,0f)
    val previous=WipeTransition.start(1000,-1,760f);assertEquals(-240f,previous,0f);assertEquals(760f,previous+1000,0f)
    assertTrue(WipeTransition.duration(1000,next)<220);assertEquals(220L,WipeTransition.duration(1000,1000f))
    assertEquals(1000f,WipeTransition.start(1000,1,null),0f)
  }
}
