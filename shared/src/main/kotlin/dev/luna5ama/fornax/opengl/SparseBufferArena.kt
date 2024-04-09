package dev.luna5ama.fornax.opengl

import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.objects.BufferObject

class SparseBufferArena(private val maxSize: Long) : IGLObjContainer by IGLObjContainer.Impl() {
    private val buffer = register(BufferObject.Immutable()).apply {
        allocate(maxSize, GL_SPARSE_STORAGE_BIT_ARB)
    }
    private var capacity = 0L
    private val freeList = arrayOfNulls<Block>(32)

    fun allocate(size: Long): Block {
        if (size > maxSize) {
            throw IllegalArgumentException("Requested size is too large")
        }

        val index = blockSizeIndex(size)
        var block = freeList[index]

        if (block == null) {
            block = allocateBlock(index)
        } else {
            freeList[index] = block.next
        }

        block.isUsed = true
        block.next = null

        return block
    }

    fun free(block: Block) {
        if (!block.isUsed) {
            throw IllegalArgumentException("Block is already free")
        }

        block.isUsed = false
        val index = block.sizePow
        block.next = freeList[index]
        freeList[index] = block
    }

    private fun allocateBlock(index: Int): Block {
        val blockSize = 1L shl index
        val roundPageSize = roundUpToPageSize(blockSize)
        val startOffset = capacity
        glNamedBufferPageCommitmentARB(buffer.id, startOffset, roundPageSize, true)
        capacity += roundPageSize

        val returnBlock = Block(index, startOffset, blockSize)

        var remainingSize = roundPageSize - blockSize
        while (remainingSize > 0) {
            val nextIndex = blockSizeIndex(remainingSize)
            val nextBlockSize = 1L shl nextIndex
            val nextBlock = Block(nextIndex, startOffset + blockSize, nextBlockSize)
            nextBlock.next = freeList[nextIndex]
            freeList[nextIndex] = nextBlock
            remainingSize -= nextBlockSize
        }

        return returnBlock
    }

    private fun blockSizeIndex(size: Long): Int {
        var pow = 6
        var s = size shr pow
        while (s > 0) {
            s = s shr 1
            pow++
        }
        return pow
    }

    inner class Block(
        val sizePow: Int,
        val offset: Long,
        val length: Long
    ) {
        var isUsed = false
        var next: Block? = null
    }

    private companion object {
        val PAGE_SIZE = glGetInteger(GL_SPARSE_BUFFER_PAGE_SIZE_ARB)

        fun roundUpToPageSize(size: Long): Long {
            return (size + PAGE_SIZE - 1) / PAGE_SIZE * PAGE_SIZE
        }
    }
}