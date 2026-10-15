package com.kitap.pdf

import kotlin.math.abs
import kotlin.math.max

object WipeTransition {
  fun start(width: Int,direction: Int,drag: Float?): Float = (if(direction>0) width.toFloat() else -width.toFloat())+(drag ?: 0f)
  fun duration(width: Int,from: Float,to: Float=0f)=(220*abs(from-to)/max(1,width)).toLong().coerceIn(80,220)
}
