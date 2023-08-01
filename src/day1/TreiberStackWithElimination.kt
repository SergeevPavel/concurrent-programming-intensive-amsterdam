package day1

import kotlinx.atomicfu.*
import java.util.concurrent.*

open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()

    private val eliminationArray = atomicArrayOfNulls<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val idx = randomCellIndex()
        val slot = eliminationArray[idx]
        if (!slot.compareAndSet(CELL_STATE_EMPTY, element)) {
            return false
        }

        for (i in 0..ELIMINATION_WAIT_CYCLES) {
            if (slot.compareAndSet(CELL_STATE_RETRIEVED, CELL_STATE_EMPTY)) {
                return true
            }
        }

        return slot.getAndSet(CELL_STATE_EMPTY) == CELL_STATE_RETRIEVED
    }

    override fun pop(): E? = tryPopElimination() ?: stack.pop()

    @Suppress("UNCHECKED_CAST")
    private fun tryPopElimination(): E? {
        val idx = randomCellIndex()
        val slot = eliminationArray[idx]
        return when (val element = slot.value) {
            CELL_STATE_EMPTY, CELL_STATE_RETRIEVED -> {
                null
            }

            else -> {
                if (slot.compareAndSet(element, CELL_STATE_RETRIEVED)) {
                    element as E
                } else {
                    null
                }
            }
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.size)

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state
        // to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}