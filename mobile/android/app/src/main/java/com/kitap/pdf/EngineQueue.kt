package com.kitap.pdf

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong

class EngineQueue(private val limit: Int) : PriorityBlockingQueue<Runnable>(limit,
  Comparator { first, second -> (first as Work).compareTo(second as Work) }) {
  @Synchronized override fun offer(job: Runnable): Boolean = size < limit && super.offer(job)
  class Work(val priority: Int, private val action: () -> Unit) : Runnable, Comparable<Work> {
    private val order = sequence.incrementAndGet()
    override fun run() = action()
    override fun compareTo(other: Work): Int = compareValuesBy(this, other, { it.priority }, { it.order })
    companion object { private val sequence = AtomicLong() }
  }
}
