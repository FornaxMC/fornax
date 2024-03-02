package dev.luna5ama.fornax.opengl

import dev.luna5ama.glwrapper.api.GL_MAP_PERSISTENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_UNSYNCHRONIZED_BIT
import dev.luna5ama.glwrapper.impl.BufferObject
import dev.luna5ama.glwrapper.impl.label
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class PMappedRingBuffer(private val capacity: Long, private val stamper: FrameStamps, frag: Int) {
    private val buffer = BufferObject.Immutable()
        .allocate(capacity, GL_MAP_PERSISTENT_BIT or frag)
        .label("PMappedRingBuffer#${System.identityHashCode(this)}}")
    val mapped = buffer.map(GL_MAP_PERSISTENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT or frag)

    private val allocatedFrames = ArrayDeque<AllocationFrame>()
    private val rwLock = ReentrantReadWriteLock()

    @Volatile
    private var lastUsedFrame: AllocationFrame? = null

    @Volatile
    private var currentFrame = AllocationFrame(0, 0)

    @Volatile
    private var tryWrap = false

    init {
        check(mapped.ptr.address != 0L) { "Failed to map buffer" }
    }

    fun update() {
        if (allocatedFrames.isEmpty()) {
            lastUsedFrame = null
        } else {
            while (allocatedFrames.isNotEmpty()) {
                val frame = allocatedFrames.peekFirst()
                if (frame.allocateCounter.get() == 0) {
                    if (frame.writeFlag) {
                        if (frame.stamp == null) {
                            frame.stamp = stamper.currentStamp
                            lastUsedFrame = frame
                            break
                        }
                        if (!frame.stamp!!.isDone) {
                            lastUsedFrame = frame
                            break
                        }
                    }
                    if (frame === lastUsedFrame) {
                        lastUsedFrame = null
                    }
                    check(allocatedFrames.removeFirst() == frame)
                } else {
                    lastUsedFrame = frame
                    break
                }
            }
        }

        val last = rwLock.write {
            val last = currentFrame
            var newOffset = last.offset + last.size.get()
            val lastUsed = lastUsedFrame
            if (lastUsed != null && newOffset >= lastUsed.offset) {
                return
            }
            var warpBit = last.warpBit
            if (tryWrap || newOffset > capacity) {
                newOffset = 0
                warpBit = warpBit xor 1
            }
            tryWrap = false
            currentFrame = AllocationFrame(newOffset, warpBit)
            last
        }
        allocatedFrames.addLast(last)
    }

    fun allocate(size: Long): Block? {
        rwLock.read {
            val frame = currentFrame
            var globalOffset: Long
            do {
                val frameOffset: Long = frame.size.get()
                globalOffset = frame.offset + frameOffset
                if (globalOffset + size > capacity) {
                    tryWrap = true
                    return null
                }
                val lastUsed = lastUsedFrame
                if (lastUsed != null && lastUsed.warpBit != frame.warpBit && globalOffset + size >= lastUsed.offset) {
                    return null
                }
            } while (!frame.size.compareAndSet(frameOffset, frameOffset + size))
            return Block(frame, globalOffset, size)
        }
    }

    inner class Block internal constructor(
        private val frame: AllocationFrame,
        val offset: Long,
        val size: Long
    ) {
        init {
            frame.allocateCounter.incrementAndGet()
        }

        val buffer: BufferObject
            get() = this@PMappedRingBuffer.buffer

        val ptr = mapped.ptr + offset

        @Volatile
        private var a = true

        fun free(write: Boolean) {
             check(a)
            a = false
            frame.free(write)
        }
    }

    internal class AllocationFrame(val offset: Long, val warpBit: Int) {
        val size = AtomicLong(0)
        val allocateCounter = AtomicInteger(0)

        @Volatile
        var writeFlag = false

        var stamp: FrameStamps.Stamp? = null

        fun free(write: Boolean) {
            if (write) {
                writeFlag = true
            }
            allocateCounter.decrementAndGet()
        }
    }
}