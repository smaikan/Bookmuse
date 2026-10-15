package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class EngineQueueTest {
  @Test fun visibleWorkPrecedesSpeculationAndQueueIsBounded() {
    val queue = EngineQueue(2)
    val prefetch = EngineQueue.Work(2) { }
    val visible = EngineQueue.Work(0) { }
    assertTrue(queue.offer(prefetch)); assertTrue(queue.offer(visible))
    assertFalse(queue.offer(EngineQueue.Work(0) { }))
    assertSame(visible, queue.poll()); assertSame(prefetch, queue.poll())
  }
  @Test fun commandsWithEqualPriorityStayInOrder() {
    val queue = EngineQueue(3)
    val save = EngineQueue.Work(0) { }; val close = EngineQueue.Work(0) { }
    queue.offer(save); queue.offer(close)
    assertSame(save, queue.poll()); assertSame(close, queue.poll())
  }
}
