package dev.luna5ama.fornax.texture

import dev.luna5ama.fornax.FornaxMod
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.util.QuadTree
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.TextureObject
import dev.luna5ama.glwrapper.impl.parameteri
import dev.luna5ama.kmogus.MemoryStack

class VirtualTextureAtlas(val format: Int) :
    IGLObjContainer by IGLObjContainer.Impl() {

    val textureSize = FornaxMod.config.virtualTextureAtlasSize

    init {
        check(textureSize and (textureSize - 1) == 0) { "Texture size must be a power of 2" }
    }

    val texture = register(TextureObject.Texture2D(GL_TEXTURE_2D))
    val pageSize: Int

    init {
        val n = glGetInternalformati(GL_TEXTURE_2D, format, GL_NUM_VIRTUAL_PAGE_SIZES_ARB)
        var idx = -1
        var minSize = Int.MAX_VALUE

        MemoryStack {
            val xSize = malloc(4L * n).ptr
            val ySize = malloc(4L * n).ptr

            glGetInternalformativ(GL_TEXTURE_2D, format, GL_VIRTUAL_PAGE_SIZE_X_ARB, n, xSize)
            glGetInternalformativ(GL_TEXTURE_2D, format, GL_VIRTUAL_PAGE_SIZE_Y_ARB, n, ySize)

            for (i in 0 until n) {
                val x = xSize.getInt(i * 4L)
                val y = ySize.getInt(i * 4L)
                if (x == y && x < minSize) {
                    idx = i
                    minSize = x
                }
            }
        }

        check(minSize != Int.MAX_VALUE) { "No suitable virtual page size found" }
        check(idx != -1) { "No suitable virtual page size found" }

        pageSize = minSize
        texture.parameteri(GL_TEXTURE_SPARSE_ARB, GL_TRUE)
            .parameteri(GL_VIRTUAL_PAGE_SIZE_INDEX_ARB, idx)
            .allocate(0, format, textureSize, textureSize)
    }

    private val quadTree = QuadTree<Block>()
    private val freeList = FreeList()

    fun allocate(idx: Int): Block {
        var block = freeList.pop(idx)

        if (block == null) {
            for (i in (idx - 1) downTo 0) {
                block = freeList.pop(i)
                if (block != null) {
                    var j = i
                    while (block!!.level > idx) {
                        block.split()
                        block = freeList.pop(++j) ?: error("Failed to allocate block (split)")
                    }
                    break
                }
            }
        }

        block ?: error("Failed to allocate block (no free blocks?)")

        block.commit(true)

        return block
    }

    fun free(block: Block) {
        block.state = BlockState.FREE
        val combined = combine(block)
        if (combined.size >= pageSize) {
            combined.commit(false)
            freeList.push(combined)
        }
    }

    private fun combine(block: Block): Block {
        val parent = block.parent
        if (parent == null) {
            return block
        }

        val c11 = parent.c11
        val c12 = parent.c12
        val c21 = parent.c21
        val c22 = parent.c22

        if (c11.isNullorFree && c12.isNullorFree && c21.isNullorFree && c22.isNullorFree) {
            parent.state = BlockState.FREE

            if (c11 != null) freeList.remove(c11)
            if (c12 != null) freeList.remove(c12)
            if (c21 != null) freeList.remove(c21)
            if (c22 != null) freeList.remove(c22)

            parent.c11 = null
            parent.c12 = null
            parent.c21 = null
            parent.c22 = null

            return combine(parent)
        }

        return block
    }

    private val Block?.isNullorFree: Boolean
        get() = this == null || state == BlockState.FREE

    inner class Block(
        parent: Block?,
        val level: Int,
        val offsetX: Int,
        val offsetY: Int,
        val size: Int,
    ) : QuadTree.Node<Block>(parent) {
        var state = BlockState.FREE

        var prev: Block? = null
        var next: Block? = null

        fun split() {
            check(level > 0) { "Cannot split block at max level" }
            check(state != BlockState.SPLIT) { "Block already split" }
            assert(state == BlockState.USED)

            val newSize = size / 2
            val newLevel = level + 1

            state = BlockState.SPLIT
            c11 = Block(this, newLevel, offsetX, offsetY, newSize)
            c12 = Block(this, newLevel, offsetX + newSize, offsetY, newSize)
            c21 = Block(this, newLevel, offsetX, offsetY + newSize, newSize)
            c22 = Block(this, newLevel, offsetX + newSize, offsetY + newSize, newSize)

            freeList.push(c22!!)
            freeList.push(c21!!)
            freeList.push(c12!!)
            freeList.push(c11!!)

            freeList.remove(this)

            if (freeList[level] == this) {
                freeList[level] = null
                freeList
            }
        }

        fun commit(commit: Boolean) {
            var block = this
            while (block.size < pageSize) {
                block = block.parent!!
            }
            glTexturePageCommitmentEXT(
                texture.id,
                0,
                block.offsetX,
                block.offsetY,
                0,
                block.size,
                block.size,
                1,
                commit
            )
        }
    }

    enum class BlockState {
        SPLIT, USED, FREE
    }

    private inner class FreeList {
        private val heads = arrayOfNulls<Block>(textureSize.countTrailingZeroBits() + 1)

        operator fun get(idx: Int) = heads[idx]
        operator fun set(idx: Int, block: Block?) {
            heads[idx] = block
        }

        fun linkNode(a: Block?, b: Block?) {
            a?.next = b
            b?.prev = a
        }

        fun remove(block: Block) {
            val prev = block.prev
            val next = block.next

            if (prev != null) {
                if (next != null && next.state == BlockState.FREE) {
                    freeList.linkNode(prev, next)
                } else {
                    freeList.linkNode(prev, null)
                }
            }

            if (next != null) {
                if (prev != null && prev.state == BlockState.FREE) {
                    freeList.linkNode(prev, next)
                } else {
                    freeList.linkNode(null, next)
                }
            }

            block.prev = null
            block.next = null
        }

        fun push(block: Block) {
            assert(block.state == BlockState.FREE)

            val idx = block.level
            val head = heads[idx]
            if (head == null) {
                heads[idx] = block
                linkNode(null, block)
                linkNode(block, null)
                return
            }

            linkNode(block, head)
            heads[idx] = block
        }

        fun pop(idx: Int): Block? {
            val head = heads[idx] ?: return null

            assert(head.state == BlockState.FREE)
            assert(head.prev == null)

            val next = head.next
            linkNode(null, next)

            head.state = BlockState.USED
            head.prev = null
            head.next = null

            return head
        }
    }
}