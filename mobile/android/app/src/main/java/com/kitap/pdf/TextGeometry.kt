package com.kitap.pdf

import kotlin.math.*

/** Packed immutable metadata remains native; Unicode scalar indices are PDFium indices, not UTF-16 offsets. */
class TextGeometry(val data: FloatArray,private val lease: ByteBudget.Lease) : AutoCloseable {
  val count get()=data.size/19
  private var refs=1
  @Synchronized fun retain(): TextGeometry { check(refs>0);refs++;return this }
  @Synchronized override fun close() { check(refs>0);if(--refs==0) lease.close() }
  fun code(index: Int)=data[index*19].toInt()
  private fun word(index: Int): Boolean {
    val c=code(index)
    val type=Character.getType(c)
    return Character.isLetterOrDigit(c) || type==6 || type==8 || c==39 || c==0x2019
  }
  fun wordRange(index: Int): IntRange {
    require(index in 0 until count)
    var a=index;var b=index
    if(word(index)) { while(a>0 && word(a-1)) a--;while(b<count-1 && word(b+1)) b++ }
    return a..b
  }
  fun hit(x: Float,y: Float,tolerance: Float=.006f): Int {
    var best=-1;var distance=Float.MAX_VALUE
    for(i in 0 until count) {
      if(code(i)<=32) continue
      val o=i*19+11
      val left=minOf(data[o],data[o+2],data[o+4],data[o+6]);val right=maxOf(data[o],data[o+2],data[o+4],data[o+6])
      val top=minOf(data[o+1],data[o+3],data[o+5],data[o+7]);val bottom=maxOf(data[o+1],data[o+3],data[o+5],data[o+7])
      val dx=max(0f,max(left-x,x-right));val dy=max(0f,max(top-y,y-bottom));val d=dx*dx+dy*dy
      if(dx<=tolerance && dy<=tolerance && d<distance) { best=i;distance=d }
    }
    return best
  }
  fun quad(index: Int,display: Boolean)=data.copyOfRange(index*19+if(display) 11 else 3,index*19+if(display) 19 else 11)
  fun ranges(start: Int,end: Int,display: Boolean): List<FloatArray> {
    require(start>=0 && end>=start && end<count && end-start<4096) { "Tek seçim en fazla 4096 karakter olabilir" }
    val output=mutableListOf<FloatArray>();var current: FloatArray?=null;var currentPdf: FloatArray?=null;var angle=0f
    for(i in start..end) {
      require(code(i)!=0 || data[i*19+1]==4f) { "Pasajda Unicode eşlemesi olmayan karakter var. Bölge işaretle kullanılabilir." }
      if(code(i)<=32) { if(code(i)==10 || code(i)==13) { current=null;currentPdf=null };continue }
      require(data[i*19+1]==0f) { "Bu pasajın eğik yazısı veya Unicode eşlemesi güvenilir değil. Bölge işaretle kullanılabilir." }
      val pdf=quad(i,false);val q=if(display) quad(i,true) else pdf;val a=data[i*19+2]
      val old=current
      val mergedPdf=if(currentPdf!=null && abs(a-angle)<.015f) merge(currentPdf,pdf) else null
      val merged=if(old!=null && mergedPdf!=null) if(display) merge(old,q,false) else mergedPdf else null
      if(merged==null) { output.add(q);current=q;currentPdf=pdf;angle=a } else { output[output.lastIndex]=merged;current=merged;currentPdf=mergedPdf }
      require(output.size<=512) { "Seçim çok parçalı; daha kısa bir pasaj seçin" }
    }
    require(output.isNotEmpty()) { "Bu bölgede seçilebilir metin yok. Bölge işaretle kullanılabilir." }
    return output
  }
  companion object {
    // Baseline-aligned envelope; gaps between columns and different baselines always split.
    fun merge(a: FloatArray,b: FloatArray,checkSpacing: Boolean=true): FloatArray? {
      val dx=a[2]-a[0];val dy=a[3]-a[1];val length=hypot(dx,dy)
      if(length<1e-8) return null
      val ux=dx/length;val uy=dy/length;val vx=-uy;val vy=ux
      fun project(q: FloatArray,u: Boolean)=FloatArray(4) { j -> q[j*2]*(if(u) ux else vx)+q[j*2+1]*(if(u) uy else vy) }
      val au=project(a,true);val av=project(a,false);val bu=project(b,true);val bv=project(b,false)
      val h=max(av.max()-av.min(),bv.max()-bv.min())
      if(h<1e-8 || (checkSpacing && abs((av.max()+av.min())/2-(bv.max()+bv.min())/2)>h*.35f)) return null
      val gap=max(au.min()-bu.max(),bu.min()-au.max())
      if(checkSpacing && gap>h*1.5f) return null
      val low=min(au.min(),bu.min());val high=max(au.max(),bu.max());val bottom=min(av.min(),bv.min());val top=max(av.max(),bv.max())
      val forward=(a[6]-a[0])*vx+(a[7]-a[1])*vy>=0
      val baseline=if(forward) bottom else top;val cap=if(forward) top else bottom
      return floatArrayOf(ux*low+vx*baseline,uy*low+vy*baseline,ux*high+vx*baseline,uy*high+vy*baseline,
        ux*high+vx*cap,uy*high+vy*cap,ux*low+vx*cap,uy*low+vy*cap)
    }
  }
}
