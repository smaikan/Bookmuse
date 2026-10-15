package com.kitap.pdf

/** Virtual scale remembers travel below fit without changing the durable viewport. */
class OverviewPinch {
  var zoom = 1f; private set
  var armed = false; private set
  val displayZoom get() = zoom.coerceIn(1f,5f)
  val preview get() = ((1f-zoom)/.18f).coerceIn(0f,1f)
  fun begin(initial: Float) { zoom=initial.coerceIn(1f,5f);armed=false }
  fun scale(factor: Float) {
    if(!factor.isFinite() || factor<=0f) return
    zoom=(zoom*factor).coerceIn(.5f,5f)
    if(zoom<=.82001f) armed=true else if(zoom>=.89999f) armed=false
  }
  fun cancel() { begin(1f) }
}
