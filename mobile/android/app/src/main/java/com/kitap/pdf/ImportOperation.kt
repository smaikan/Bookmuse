package com.kitap.pdf

import java.io.Closeable
import java.util.concurrent.CancellationException

/** Cancellation never queues behind a provider read. Commit is an explicit point of no return. */
class ImportOperation {
  private var cancelled = false
  private var committed = false
  private var source: Closeable? = null
  @Synchronized fun check() { if (cancelled) throw CancellationException("Ekleme iptal edildi") }
  @Synchronized fun attach(resource: Closeable?) { check(); source = resource }
  fun cancel(): Boolean {
    val resource: Closeable?
    synchronized(this) {
      if (committed) return false
      cancelled = true
      resource = source
      source = null
    }
    // Closing a blocked provider stream can be slow; the caller must run this off the UI thread.
    try { resource?.close() } catch (_: Exception) { }
    return true
  }
  @Synchronized fun commit(action: () -> Unit) {
    check()
    action()
    committed = true
  }
}
