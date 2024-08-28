package dev.luna5ama.fornax.opengl

import dev.fastmc.common.pollEach
import dev.luna5ama.glwrapper.api.GL_MAP_PERSISTENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_UNSYNCHRONIZED_BIT
import dev.luna5ama.glwrapper.objects.BufferObject
import dev.luna5ama.kmogus.Ptr
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class PersistentRingBuffer(capacity2Pow: Int, frag: Int) :
    IGLObjContainer by IGLObjContainer.Impl() {
    private val capacity = 1L shl capacity2Pow
    private val capacityMask = (1L shl capacity2Pow) - 1

    private val buffer = register(BufferObject.Immutable()).apply {
        allocate(capacity, GL_MAP_PERSISTENT_BIT or frag)
        label("PersistentRingBuffer#${System.identityHashCode(this)}}")
    }
    val mapped = buffer.map(GL_MAP_PERSISTENT_BIT or GL_MAP_UNSYNCHRONIZED_BIT or frag)

    init {
        check(mapped.ptr.address != 0L) { "Failed to map buffer" }
    }

    private val allocateOffset = AtomicLong(0)
    private val freeOffset = AtomicLong(capacity)
    private val awaiting = ConcurrentLinkedDeque<Pair<Long, Continuation<Unit>>>()

    private val frameRWLock = ReentrantReadWriteLock()
    @Volatile
    private var currentFrame = AllocationFrame(0L)
    private val allocatedFrames = ArrayDeque<AllocationFrame>()

    fun update() {
        frameRWLock.write {
            val lastFrame = currentFrame
            if (lastFrame.allocated.get() == 0L) return@write
            allocatedFrames.add(lastFrame)
            currentFrame = AllocationFrame(lastFrame.startOffset + lastFrame.allocated.get())
        }

        var frame = allocatedFrames.peekFirst()
        while (frame != null) {
            val frameAllocated = frame.allocated.get()
            assert(frameAllocated > 0L)
            assert(frame.startOffset < freeOffset.get())
            if (frameAllocated > frame.freed.get()) break

            freeOffset.getAndAdd(frameAllocated)

            allocatedFrames.pollFirst()
            frame = allocatedFrames.peekFirst()
        }

        var freed = freeOffset.get() - allocateOffset.get()
        if (freed > 0L) {
            awaiting.pollEach { (size, it) ->
                it.resumeWith(Result.success(Unit))
                freed -= size
                if (freed <= 0L) return
            }
        }
    }

    suspend fun allocate(size: Long): Block {
        while (true) {
            val block = tryAllocate(size)
            if (block != null) return block
            suspendCoroutine {
                awaiting.add(size to it)
            }
        }
    }

    fun tryAllocate(size: Long): Block? {
        require(size > 0) { "Size must be positive" }

        var startOffset: Long
        var startPadding: Long
        var endOffset: Long

        val frame = frameRWLock.read {
            do {
                startOffset = allocateOffset.get()
                startPadding = 0L
                endOffset = startOffset + size
                val newActualOffset = endOffset and capacityMask
                if (newActualOffset != 0L && newActualOffset < size) {
                    // Not enough contiguous space at the end, requires padding and wrapping
                    endOffset -= newActualOffset
                    startPadding = endOffset - startOffset // Padding until the end of the buffer
                    endOffset += size
                }
                if (endOffset > freeOffset.get()) return null
                assert(startOffset >= 0)
                assert(startOffset < endOffset)
                assert(startPadding < size)
                assert(startOffset + startPadding + size == endOffset)
            } while (!allocateOffset.compareAndSet(startOffset, endOffset))
            currentFrame.allocated.getAndAdd(startPadding + size)
            currentFrame
        }
        assert(frame.startOffset <= startOffset)
        val block = BlockImpl(frame, startOffset, startPadding, size)

        return block
    }

    interface Block {
        val offset: Long
        val size: Long
        val bufferObject: BufferObject
        val ptr: Ptr
        fun free()
    }

    private inner class BlockImpl(
        val frame: AllocationFrame,
        startOffset: Long,
        val startPadding: Long,
        override val size: Long,
    ) : Block {

        override val offset = (startOffset + startPadding) and capacityMask

        override val bufferObject: BufferObject
            get() = this@PersistentRingBuffer.buffer

        override val ptr = mapped.ptr + offset

        init {
            assert(offset + size <= capacity)
        }

        @Volatile
        private var free = false

        override fun free() {
            check(!free) { "Block is already freed" }
            free = true
            frame.freed.getAndAdd(startPadding + size)
        }
    }

    private class AllocationFrame(val startOffset: Long) {
        val allocated = AtomicLong(0)
        val freed = AtomicLong(0)
    }
}