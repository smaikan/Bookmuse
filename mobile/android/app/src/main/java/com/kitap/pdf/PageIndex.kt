package com.kitap.pdf

object PageIndex {
  fun requireValid(page: Int, count: Int): Int {
    require(count > 0 && page >= 0 && page < count) { "Sayfa numarası kitabın sınırları dışında" }
    return page
  }
}
