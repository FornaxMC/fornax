package dev.luna5ama.fornax.texture

import dev.luna5ama.fornax.FornaxMod
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.util.OpenLinkedList
import dev.luna5ama.fornax.util.QuadTree
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.impl.TextureObject
import dev.luna5ama.glwrapper.impl.parameteri
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MemoryStack
import java.util.*

class VirtualTextureAtlas(val format: Int) :
    IGLObjContainer by IGLObjContainer.Impl() {

    val textureSize = FornaxMod.config.virtualTextureAtlasSize
    private val gcBlockLevel = 2

    init {
        check(textureSize and (textureSize - 1) == 0) { "Texture size must be a power of 2" }
    }

    val sparseTexture = FornaxMod.config.sparseTexture
    val texture = register(TextureObject.Texture2D(GL_TEXTURE_2D))
    val pageSize: Int

    init {
        if (sparseTexture) {
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
                .allocate(1, format, textureSize, textureSize)
        } else {
            pageSize = -1
            texture.allocate(1, format, textureSize, textureSize)
        }
    }

    private val totalLevels = textureSize.countTrailingZeroBits() + 1
    private val quadTree = QuadTree<Block>()
    private val levelBlockLists = Array(totalLevels) { OpenLinkedList(Block::prevLevel, Block::nextLevel) }
        private val freeLists = Array(totalLevels) { OpenLinkedList(Block::prevFree, Block::nextFree) }

    init {
        val rootBlock = Block(null, 0, 0, 0, 0, textureSize)
        quadTree.root = rootBlock
        levelBlockLists[0].pushFront(rootBlock)
        freeLists[0].pushFront(rootBlock)
    }

    fun gc() {
        var maxFragmentBlock: Block? = null
        var maxFragV = 0

        var blockIter = levelBlockLists[gcBlockLevel].head
        while (blockIter != null) {
            val fragmentV = blockIter.fragmentSize()
            if (fragmentV > maxFragV) {
                maxFragV = fragmentV
                maxFragmentBlock = blockIter
            }
            blockIter = blockIter.nextLevel
        }

        if (maxFragmentBlock != null) {
            val collected = Array(totalLevels - gcBlockLevel) { mutableListOf<Block>() }
            collectBlock(collected, maxFragmentBlock)

            val targetBlock = allocate(gcBlockLevel)!!.block
            val localFreeList = Array(totalLevels - gcBlockLevel) { ArrayDeque<Block>() }
            localFreeList[0].add(targetBlock)

            for (srcIdx in collected.indices) {
                val list = collected[srcIdx]
                for (blockSrc in list) {
                    assert(blockSrc.commit)
                    assert(blockSrc.state == BlockState.USED)
                    assert(blockSrc.level == srcIdx + gcBlockLevel)
                    var blockDst = localFreeList[srcIdx].pollFirst()

                    if (blockDst == null) {
                        for (j in (srcIdx - 1) downTo 0) {
                            blockDst = localFreeList[j].pollFirst()
                            if (blockDst != null) {
                                var splitI = j
                                while (blockDst!!.level < blockSrc.level) {
                                    blockDst.split()
                                    val freeList = localFreeList[++splitI]
                                    freeList.addLast(blockDst.c12!!)
                                    freeList.addLast(blockDst.c21!!)
                                    freeList.addLast(blockDst.c22!!)
                                    blockDst = blockDst.c11!!
                                }
                                break
                            }
                        }
                    }

                    blockDst!!.commit(true)
                    blockDst.state = BlockState.USED

                    freeLists[blockDst.level].remove(blockDst)

                    assert(blockDst.level == srcIdx + gcBlockLevel)
                    assert(blockSrc.size == blockDst.size)

                    texture.copyTo(
                        texture, 0, blockSrc.offsetX, blockSrc.offsetY,
                        0, blockDst.offsetX, blockDst.offsetY,
                        blockSrc.size, blockSrc.size
                    )
                    val srcRef = blockSrc.ref
                    srcRef.block = blockDst
                    blockSrc.ref = BlockRef(blockSrc)
                    blockDst.ref = srcRef

                    free(blockSrc)
                }
            }
        }
    }

    private fun collectBlock(list: Array<MutableList<Block>>, block: Block) {
        if (block.state == BlockState.USED) {
            list[block.level - gcBlockLevel].add(block)
        } else {
            val c11 = block.c11
            val c12 = block.c12
            val c21 = block.c21
            val c22 = block.c22
            if (c11 != null) collectBlock(list, c11)
            if (c12 != null) collectBlock(list, c12)
            if (c21 != null) collectBlock(list, c21)
            if (c22 != null) collectBlock(list, c22)
        }
    }

    fun idxForSize(size: Int): Int {
        var idx = 0
        var s = textureSize
        while (s > size) {
            s = s shr 1
            idx++
        }
        return idx
    }

    fun allocate(idx: Int): BlockRef? {
        var block = freeLists[idx].popFront()

        if (block == null) {
            for (i in (idx - 1) downTo 0) {
                block = freeLists[i].popFront()
                if (block != null) {
                    var j = i
                    while (block!!.level < idx) {
                        block.split()
                        block = freeLists[++j].popFront() ?: error("Failed to allocate block (split)")
                    }
                    break
                }
            }
        }

        block?.commit(true)
        block?.state = BlockState.USED

        return block?.ref
    }

    private val clearPtr = Arr.malloc(4L).apply {
        ptr.setByteInc(-1)
            .setByteInc(0)
            .setByteInc(-1)
            .setByteInc(-1)
    }

    fun free(blockRef: BlockRef) {
        val block = blockRef.block
        free(block)
    }

    private fun free(block: Block) {
        block.state = BlockState.FREE
        val combined = combine(block)
        if (combined.size >= pageSize) {
            combined.commit(false)
            freeLists[combined.level].pushBack(combined)
        } else {
            glClearTexSubImage(
                texture.id,
                0,
                combined.offsetX,
                combined.offsetY,
                0,
                combined.size,
                combined.size,
                1,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                clearPtr.ptr
            )
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

            val cLevel = parent.level + 1
            val levelBlockList = levelBlockLists[cLevel]
            val freeList = freeLists[cLevel]

            if (c11 != null) {
                levelBlockList.remove(c11)
                freeList.remove(c11)
            }
            if (c12 != null) {
                levelBlockList.remove(c12)
                freeList.remove(c12)
            }
            if (c21 != null) {
                levelBlockList.remove(c21)
                freeList.remove(c21)
            }
            if (c22 != null) {
                levelBlockList.remove(c22)
                freeList.remove(c22)
            }

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

    class BlockRef(var block: Block)

    inner class Block(
        parent: Block?,
        val id: Int,
        val level: Int,
        val offsetX: Int,
        val offsetY: Int,
        val size: Int,
    ) : QuadTree.Node<Block>(parent) {
        var prevFree: Block? = null
        var nextFree: Block? = null
        var prevLevel: Block? = null
        var nextLevel: Block? = null

        var ref = BlockRef(this)

        var commit = false
        var state = BlockState.FREE

        val totalSize: Int
            get() = size * size

        fun fragmentSize(): Int {
            when (state) {
                BlockState.USED -> {
                    return totalSize
                }
                BlockState.FREE -> {
                    return 0
                }
                BlockState.SPLIT -> {
                    val s11 = c11!!.fragmentSize()
                    val s12 = c12!!.fragmentSize()
                    val s21 = c21!!.fragmentSize()
                    val s22 = c22!!.fragmentSize()
                    val sum = s11 + s12 + s21 + s22
                    if (sum == totalSize || sum == 0) {
                        return 0
                    }
                    return sum
                }
            }
        }

        fun split() {
            check(size > 1) { "Cannot split block at max level" }
            check(state != BlockState.SPLIT) { "Block already split" }

            val newSize = size / 2
            val newLevel = level + 1
            val newID = id * 4

            state = BlockState.SPLIT
            val n11 = Block(this, newID, newLevel, offsetX, offsetY, newSize)
            val n12 = Block(this, newID + 1, newLevel, offsetX + newSize, offsetY, newSize)
            val n21 = Block(this, newID + 2, newLevel, offsetX, offsetY + newSize, newSize)
            val n22 = Block(this, newID + 3, newLevel, offsetX + newSize, offsetY + newSize, newSize)

            c11 = n11
            c12 = n12
            c21 = n21
            c22 = n22

            val levelBlockList = levelBlockLists[newLevel]
            val freeList = freeLists[newLevel]
            freeList.pushBack(n11)
            freeList.pushBack(n12)
            freeList.pushBack(n21)
            freeList.pushBack(n22)

            levelBlockList.pushBack(n11)
            levelBlockList.pushBack(n12)
            levelBlockList.pushBack(n21)
            levelBlockList.pushBack(n22)

            freeLists[level].remove(this)
        }

        fun commit(commit: Boolean) {
            if (!sparseTexture) return

            var block = this
            this.commit = commit
            while (block.size < pageSize) {
                block = block.parent!!
                block.commit = commit
                block.c11?.commit = commit
                block.c12?.commit = commit
                block.c21?.commit = commit
                block.c22?.commit = commit
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
}