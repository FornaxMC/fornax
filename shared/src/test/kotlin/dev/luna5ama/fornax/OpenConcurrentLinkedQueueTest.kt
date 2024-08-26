package dev.luna5ama.fornax

import dev.luna5ama.fornax.util.OpenConcurrentLinkedQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for [OpenConcurrentLinkedQueue].
 */
class OpenConcurrentLinkedQueueTest {
    private class Foo(val v: Int) {
        val next: Foo? = null
    }

    private class TestInstance {
        val queue = OpenConcurrentLinkedQueue(Foo(0), Foo::class.java.getDeclaredField("next"))
        val set = Collections.newSetFromMap<Foo>(ConcurrentHashMap())
        val total = AtomicInteger()

        fun enqueue1() {
            queue.enqueue(Foo(0))
            total.incrementAndGet()
        }

        fun dequeue1() {
            val e = queue.dequeue()
            if (e != null) {
                set.add(e)
            }
        }

        fun enqueueN(n: Int) {
            repeat(n) {
                queue.enqueue(Foo(0))
            }
            total.addAndGet(n)
        }

        fun dequeueN() {
            var e = queue.dequeue()
            while (e != null) {
                set.add(e)
                e = queue.dequeue()
            }
        }

        fun check() {
            assertEquals(0, queue.size, "queue.size")
            assertEquals(total.get(), set.size, "set.size")
        }
    }


    private val perThread = 100_000
    private val nThreads = Runtime.getRuntime().availableProcessors()

    @Test
    fun enqueue1T_dequeue1T_1() {
        val instance = TestInstance()
        instance.enqueueN(perThread)
        instance.dequeueN()
        instance.check()
    }

    @Test
    fun enqueue1T_dequeue1T_2() {
        val instance = TestInstance()
        repeat(perThread) {
            instance.enqueue1()
            instance.dequeue1()
        }
        instance.check()
    }

    @Test
    fun enqueue1T_dequeueNT() {
        val instance = TestInstance()

        instance.enqueueN(nThreads * perThread)

        List(nThreads) {
            Thread {
                instance.dequeueN()
            }
        }.startAndJoin()

        instance.check()
    }

    @Test
    fun enqueueNT_dequeue1T() {
        val instance = TestInstance()

        List(nThreads) {
            Thread {
                instance.enqueueN(perThread)
            }
        }.startAndJoin()

        instance.dequeueN()
        instance.check()
    }

    @Test
    fun enqueue1T_dequeueNT_concurrent() {
        val instance = TestInstance()

        val enqueueThread = Thread {
            instance.enqueueN(nThreads * perThread)
        }

        val dequeThreads = List(nThreads) {
            Thread {
                while (enqueueThread.isAlive) {
                    instance.dequeueN()
                }
            }
        }

        (dequeThreads + enqueueThread).onEach {
            it.start()
        }.onEach {
            it.join()
        }

        instance.check()
    }

    @Test
    fun enqueueNT_dequeue1T_concurrent() {
        val instance = TestInstance()

        val enqueueThreads = List(nThreads) {
            Thread {
                instance.enqueueN(perThread)
            }
        }
        val dequeueThread = Thread {
            while (enqueueThreads.any { it.isAlive }) {
                instance.dequeueN()
            }
        }

        (enqueueThreads + dequeueThread).startAndJoin()

        instance.check()
    }

    @Test
    fun enqueueNT_dequeueNT() {
        val instance = TestInstance()

        val enqueueThreads = List(nThreads) {
            Thread {
                instance.enqueueN(perThread)
            }
        }
        val dequeueThreads = List(nThreads) {
            Thread {
                instance.dequeueN()
            }
        }

        enqueueThreads.startAndJoin()
        dequeueThreads.startAndJoin()

        instance.check()
    }

    @Test
    fun enqueueNT_dequeueNT_concurrent() {
        val instance = TestInstance()

        val enqueueThreads = List(nThreads) {
            Thread {
                instance.enqueueN(perThread)
            }
        }
        val dequeueThreads = List(nThreads) {
            Thread {
                instance.dequeueN()
            }
        }

        (enqueueThreads + dequeueThreads).startAndJoin()

        instance.check()
    }

    @Test
    fun casSort() {
        val instance = TestInstance()
        val counter = AtomicInteger()

        val enqueueThreads = List(nThreads) {
            Thread {
                repeat(perThread) {
                    val foo = Foo(counter.getAndIncrement())
                    while (!instance.queue.enqueueConditional(foo) {
                        it == null && foo.v == 0 || it != null && it.v + 1 == foo.v
                    }) {
                        // retry
                    }
                }
                instance.total.addAndGet(perThread)
            }
        }

        enqueueThreads.startAndJoin()

        var e = instance.queue.dequeue()
        while (e != null) {
            instance.set.add(e)
            val new = instance.queue.dequeue() ?: break
            assertEquals(e.v + 1, new.v, "e.v + 1 == new.v")
            e = new
        }

        instance.check()
    }

    private fun List<Thread>.startAndJoin() {
        onEach {
            it.start()
        }.onEach {
            it.join()
        }
    }
}