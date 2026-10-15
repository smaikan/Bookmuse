package com.kitap.pdf

/** Counts owned buffers including in-flight allocations; does not claim to bound total PSS. */
class ByteBudget(val limit: Long) {
  @Volatile var used: Long = 0L
    private set

  init { require(limit > 0) }

  @Synchronized fun reserve(bytes: Long): Lease? {
    require(bytes > 0)
    if (bytes > limit - used) return null
    used += bytes
    return Lease(this, bytes)
  }

  @Synchronized private fun release(bytes: Long) { used -= bytes }

  class Lease internal constructor(private val owner: ByteBudget, val bytes: Long) : AutoCloseable {
    private var closed = false
    @Synchronized override fun close() {
      if (!closed) { closed = true; owner.release(bytes) }
    }
  }
}
