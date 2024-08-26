package dev.luna5ama.fornax.util

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicInteger

/**
 * A concurrent linked queue that uses the element objects as nodes.
 *
 * This implementation is based on the algorithm described in the paper:
 * <a href="http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf">
 * Simple, Fast, and Practical Non-Blocking and Blocking Concurrent Queue Algorithms</a>
 *
 * @param E the type of elements held in this collection
 * @param dummyObject a dummy object to initialize the queue
 * @param nextField the field of the element class that points to the next element
 */
@Suppress("DEPRECATION")
class OpenConcurrentLinkedQueue<E>(private val dummyObject: E, nextField: Field) {
    private val nextOffset = UNSAFE.objectFieldOffset(nextField)

    @Volatile private var headRef = dummyObject
    @Volatile private var tailRef = dummyObject
    private val sizeCounter = AtomicInteger()

    val size: Int
        get() = sizeCounter.get()

    fun enqueueConditional(e: E, predicate: (E?) -> Boolean): Boolean {
        var tail: E
        do {
            tail = tailRef
            val next = tail.next
            if (tail === tailRef) {
                if (next === null) {
                    if (!predicate(tail.takeIf { it !== dummyObject })) {
                        return false
                    }
                    if (tail.casNext(next, e)) {
                        break
                    }
                } else {
                    casTail(tail, next)
                }
            }
        } while (true)
        sizeCounter.incrementAndGet()
        casTail(tail, e)
        return true
    }

    fun enqueue(e: E) {
        var tail: E
        do {
            tail = tailRef
            val next = tail.next
            if (tail === tailRef) {
                if (next === null) {
                    if (tail.casNext(next, e)) {
                        break
                    }
                } else {
                    casTail(tail, next)
                }
            }
        } while (true)
        sizeCounter.incrementAndGet()
        casTail(tail, e)
    }

    fun dequeue(): E? {
        var head: E
        var tail: E
        var next: E?
        var value: E
        do {
            head = headRef
            tail = tailRef
            next = head.next
            if (head === headRef) {
                if (head === tail) {
                    if (next === null) {
                        return null
                    }
                    casTail(tail, next)
                } else {
                    value = next!!
                    if (casHead(head, next)) {
                        break
                    }
                }
            }
        } while (true)
        sizeCounter.decrementAndGet()
        assert(value !== dummyObject)
        return value
    }

    private fun casHead(oldHead: E?, newHead: E?): Boolean {
        return UNSAFE.compareAndSwapObject(this, HEAD_OFFSET, oldHead, newHead)
    }

    private fun casTail(oldTail: E?, newTail: E?): Boolean {
        return UNSAFE.compareAndSwapObject(this, TAIL_OFFSET, oldTail, newTail)
    }

    @Suppress("UNCHECKED_CAST")
    val E.next: E?
        get() = UNSAFE.getObjectVolatile(this, nextOffset) as E?

    private fun E.casNext(oldNext: E?, newNext: E?): Boolean {
        return UNSAFE.compareAndSwapObject(this, nextOffset, oldNext, newNext)
    }

    private companion object {
        @JvmField
        val UNSAFE = run {
            val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            theUnsafe.get(null) as Unsafe
        }

        val HEAD_OFFSET = UNSAFE.objectFieldOffset(OpenConcurrentLinkedQueue::class.java.getDeclaredField("headRef"))
        val TAIL_OFFSET = UNSAFE.objectFieldOffset(OpenConcurrentLinkedQueue::class.java.getDeclaredField("tailRef"))
    }
}