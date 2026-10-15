package com.kitap.pdf

object Spread {
  fun pages(active: Int, count: Int, coverSingle: Boolean): List<Int> {
    PageIndex.requireValid(active,count)
    if (coverSingle && active==0) return listOf(0)
    val first=if(coverSingle) 1+(active-1)/2*2 else active/2*2
    return (first..minOf(first+1,count-1)).toList()
  }
  fun next(active: Int,count: Int,coverSingle: Boolean,direction: Int): Int {
    val current=pages(active,count,coverSingle)
    return if(direction>0) minOf(count-1,current.last()+1) else maxOf(0,current.first()-2)
  }
}
