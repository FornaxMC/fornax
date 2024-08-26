package dev.luna5ama.fornax.opengl

import dev.fastmc.common.pollEach
import dev.luna5ama.fornax.util.OpenConcurrentLinkedQueue
import dev.luna5ama.glwrapper.api.GL_MAP_PERSISTENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_UNSYNCHRONIZED_BIT
import dev.luna5ama.glwrapper.objects.BufferObject
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
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

    private val dummyObject = Block(0, 0, 0)
    private val allocatedBlocks = OpenConcurrentLinkedQueue(dummyObject, Block::class.java.getDeclaredField("next"))
    private val allocateOffset = AtomicLong(0)
    private val freeOffset = AtomicLong(capacity)
    private val awaiting = ConcurrentLinkedDeque<Continuation<Unit>>()
    private val updateLock = ReentrantLock()

    fun tryUpdate(): Boolean {
        if (!updateLock.tryLock()) return false
        try {
            var freed = false
            var block = allocatedBlocks.peekHead()
            while (block != null) {
                if (!block.free) break
                if (allocatedBlocks.size == 1) break
                val newFreeOffset = block.rawStartOffset + capacity

                freed = true
                freeOffset.set(newFreeOffset)

                val last = allocatedBlocks.dequeue()
                assert(last == block)
                assert(last != null)
                block = allocatedBlocks.peekHead()
                assert(block == null || last!!.rawWriteOffset + last.size == block.rawStartOffset)
            }
            if (freed) {
                val currentTail = awaiting.peekLast()
                awaiting.pollEach {
                    it.resumeWith(Result.success(Unit))
                    if (it === currentTail) return true
                }
            }
        } finally {
            updateLock.unlock()
        }
        return true
    }

    suspend fun allocate(size: Long): Block {
        while (true) {
            val block = tryAllocate(size)
            if (block != null) return block
            suspendCoroutine {
                awaiting.add(it)
            }
        }
    }

    fun tryAllocate(size: Long): Block? {
        var oldOffset: Long
        var writeOffset: Long
        var newOffset: Long
        do {
            oldOffset = allocateOffset.get()
            writeOffset = oldOffset
            newOffset = oldOffset + size
            val newActualOffset = newOffset and capacityMask
            if (newActualOffset < size) {
                // Not enough contiguous space at the end, requires padding and wrapping
                newOffset -= newActualOffset // Padding until the end
                writeOffset = newOffset // Start from the beginning
                newOffset += size
            }
            if (newOffset > freeOffset.get()) return null
            assert(oldOffset <= writeOffset || oldOffset > 0 && writeOffset < 0)
            assert(writeOffset < freeOffset.get())
        } while (!allocateOffset.compareAndSet(oldOffset, newOffset))

        val block = Block(oldOffset, writeOffset, size)
        while (!allocatedBlocks.enqueueConditional(block) {
            assert(it === dummyObject || it.rawStartOffset < block.rawStartOffset)
            it.rawWriteOffset + it.size == block.rawStartOffset
        }) {
            // Retry
        }
        return block
    }

    inner class Block internal constructor(
        internal val rawStartOffset: Long,
        internal val rawWriteOffset: Long,
        val size: Long
    ) {
        init {
            assert(rawStartOffset <= rawWriteOffset || rawStartOffset > 0 && rawWriteOffset < 0)
            assert(offset + size <= capacity)
        }

        private val next: Block? = null

        val offset get() = rawWriteOffset and capacityMask

        val bufferObject: BufferObject
            get() = this@PersistentRingBuffer.buffer

        val ptr = mapped.ptr + offset

        @Volatile
        internal var free = false

        fun free() {
            check(!free) { "Block is already freed" }
            free = true
        }
    }
}