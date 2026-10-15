package com.kitap.pdf

import org.junit.Assert.*
import org.junit.Test

class ByteBudgetTest {
  @Test fun keepsInFlightAndVisibleReservationsInsideBudget() {
    val budget = ByteBudget(100)
    val visible = checkNotNull(budget.reserve(60))
    assertNull(budget.reserve(50))
    val inflight = checkNotNull(budget.reserve(40))
    assertEquals(100L, budget.used)
    visible.close()
    visible.close()
    assertEquals(40L, budget.used)
    inflight.close()
    assertEquals(0L, budget.used)
  }
  @Test fun rejectsAllocationAboveLimitWithoutOverflow() {
    val budget = ByteBudget(Long.MAX_VALUE)
    checkNotNull(budget.reserve(Long.MAX_VALUE))
    assertNull(budget.reserve(1))
  }
}
