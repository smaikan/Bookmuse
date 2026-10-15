package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class TextGeometryTest {
  private fun quad(x: Float,y: Float)=floatArrayOf(x,y,x+5,y,x+5,y+10,x,y+10)
  private fun metadata(codes: IntArray): Pair<TextGeometry,ByteBudget> {
    val budget=ByteBudget(4*1024*1024L);val data=FloatArray(codes.size*19)
    codes.forEachIndexed { i,c -> data[i*19]=c.toFloat();val q=quad(30f+i*6,20f);q.copyInto(data,i*19+3);q.forEachIndexed { k,n -> data[i*19+11+k]=n/1000 } }
    return TextGeometry(data,budget.reserve(data.size*4L+1)!!) to budget
  }
  @Test fun turkishCombiningAndSupplementaryUnicodeKeepPdfIndices() {
    val (m,_)=metadata(intArrayOf('İ'.code,'s'.code,'t'.code,0x301,'a'.code,'n'.code,32,0x1f600))
    assertEquals(0..5,m.wordRange(2));assertEquals(7..7,m.wordRange(7));m.close()
  }
  @Test fun columnsAndDifferentBaselinesCannotBecomeOneRectangle() {
    assertNotNull(TextGeometry.merge(quad(20f,30f),quad(26f,30f)))
    assertNull(TextGeometry.merge(quad(20f,30f),quad(150f,30f)))
    assertNull(TextGeometry.merge(quad(20f,30f),quad(26f,44f)))
    val rotatedA=floatArrayOf(30f,20f,30f,25f,20f,25f,20f,20f)
    val rotatedB=floatArrayOf(30f,26f,30f,31f,20f,31f,20f,26f)
    val merged=TextGeometry.merge(rotatedA,rotatedB)!!
    assertEquals(30f,merged[0],.0001f);assertEquals(31f,merged[3],.0001f)
  }
  @Test fun retainedMetadataCannotReleaseItsBudgetWhileWorkerUsesIt() {
    val (m,budget)=metadata(intArrayOf('a'.code,'b'.code));m.retain();m.close()
    assertTrue(budget.used>0);assertEquals(0..1,m.wordRange(0));m.close();assertEquals(0,budget.used)
  }
  @Test fun invalidGeometryIsRejectedBeforeItCanBeSavedAsGoodText() {
    val (m,_)=metadata(intArrayOf('a'.code,'b'.code));m.data[19+1]=2f
    assertThrows(IllegalArgumentException::class.java) { m.ranges(0,1,false) };m.close()
  }
  @Test fun hitTestUsesNormalizedQuadAndDoesNotPickRemoteText() {
    val (m,_)=metadata(intArrayOf('a'.code,'b'.code))
    assertEquals(0,m.hit(.032f,.025f));assertEquals(-1,m.hit(.8f,.8f));m.close()
  }
  @Test fun explicitNewlinesSplitGeometryEvenIfThePdfBoxesShareARow() {
    val (m,_)=metadata(intArrayOf('a'.code,10,'b'.code))
    assertEquals(2,m.ranges(0,2,false).size);m.close()
  }
  @Test fun unknownUnicodeCannotBeSilentlyDroppedFromAQuote() {
    val (m,_)=metadata(intArrayOf('a'.code,0,'b'.code));m.data[19+1]=1f
    assertThrows(IllegalArgumentException::class.java) { m.ranges(0,2,false) }
    m.data[19+1]=4f;assertEquals(1,m.ranges(0,2,false).size);m.close()
  }
  @Test fun displayYInversionKeepsMergedBaselineOnTheCorrectSide() {
    val (m,_)=metadata(intArrayOf('a'.code,'b'.code))
    for(i in 0 until m.count) repeat(4) { corner -> val offset=i*19+12+corner*2;m.data[offset]=1-m.data[offset] }
    val merged=m.ranges(0,1,true).single()
    assertEquals(.98f,merged[1],.0001f);assertEquals(.97f,merged[5],.0001f);m.close()
  }
}
