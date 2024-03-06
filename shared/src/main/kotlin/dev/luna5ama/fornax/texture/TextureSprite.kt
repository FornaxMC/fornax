package dev.luna5ama.fornax.texture

import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.fornax.data.STexture
import dev.luna5ama.fornax.opengl.PMappedRingBuffer
import dev.luna5ama.kmogus.memcpy
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.lang.ref.SoftReference
import java.net.URI
import kotlin.math.ceil
import kotlin.math.floor

data class TextureSprite(val ref: ResourceReference) {
    val animationMeta: STexture.Animation?

    private val imagePath = URI(ref.resolve().toString() + ".png").toURL()

    private val blocks0 = mutableListOf<VirtualTextureAtlas.BlockRef?>()

    init {
        runCatching {
            imagePath.openStream().use { }
        }.onFailure {
            throw IllegalArgumentException("Texture not found: $ref", it)
        }

        val mcmetaPath = URI(ref.resolve().toString() + ".png.mcmeta").toURL()
        val sTexture = try {
            mcmetaPath.openStream()?.use {
                JSON.decodeFromString(STexture.serializer(), it.readBytes().decodeToString())
            }
        } catch (e: FileNotFoundException) {
            null
        }

        animationMeta = sTexture?.animation
    }

    private var dataCached = SoftReference<MipmapTextureData>(null)

    fun registerAtlasBlock(mipLevel: Int, block: VirtualTextureAtlas.BlockRef): VirtualTextureAtlas.BlockRef? {
        while (blocks0.size <= mipLevel) {
            blocks0.add(null)
        }
        return blocks0.set(mipLevel, block)
    }

    fun getAtlasBlock(mipLevel: Int): VirtualTextureAtlas.BlockRef? {
        return blocks0.getOrNull(mipLevel)
    }

    suspend fun loadTextureData(): MipmapTextureData {
        val cached = dataCached.get()
        if (cached != null) return cached

        @Suppress("BlockingMethodInNonBlockingContext")
        val baseImage = imagePath.openStream()!!.use {
            NativeImage.read(it)
        }

        val images = mutableListOf(baseImage)
        var currentImage = baseImage
        while (currentImage.width > 1 && currentImage.height > 1) {
            val nextImage = currentImage.half()
            images.add(nextImage)
            currentImage = nextImage
        }

        val data = MipmapTextureData(images)
        dataCached = SoftReference(data)

        return data
    }

    suspend fun getFrame(buffer: PMappedRingBuffer, tickIndex: Long): Sequence<PendingUpdateData> {
        val textureData = loadTextureData()
        if (animationMeta == null) {
            return textureData.images.asSequence()
                .mapIndexed { level, it ->
                    val mipLevelSize = it.data.len
                    val block = buffer.allocate(mipLevelSize)
                    memcpy(it.data.ptr, block.ptr, mipLevelSize)
                    PendingUpdateData(textureData, this, level, it.width, block)
                }
        } else {
            val frameTime = tickIndex / animationMeta.frametime
            val totalFrames = textureData.height / textureData.width

            fun getFrameYIndex(frameTime: Long): Int {
                return if (animationMeta.frames == null) {
                    (frameTime % totalFrames).toInt()
                } else {
                    animationMeta.frames[(frameTime % animationMeta.frames.size).toInt()]
                }
            }

            if (!animationMeta.interpolate) {
                val yIndex = getFrameYIndex(frameTime)
                return textureData.images.asSequence()
                    .mapIndexed { level, it ->
                        val mipLevelSize = it.width.toLong() * it.width * it.channels
                        val block = buffer.allocate(mipLevelSize)
                        val offset = yIndex.toLong() * it.width * it.width * it.channels
                        memcpy(it.data.ptr, offset, block.ptr, 0L, mipLevelSize)
                        PendingUpdateData(textureData, this, level, it.width, block)
                    }
            } else {
                val frameTimeD = tickIndex.toDouble() / animationMeta.frametime
                val yIndex1 = getFrameYIndex(floor(frameTimeD).toLong())
                val yIndex2 = getFrameYIndex(ceil(frameTimeD).toLong())
                val mixRatio = frameTimeD - floor(frameTimeD)
                val mul2 = (mixRatio * MIX_MUL).toInt()
                val mul1 = MIX_MUL - mul2
                return textureData.images.asSequence()
                    .mapIndexed { level, it ->
                        val mipLevelSize = it.width.toLong() * it.width * it.channels
                        val block = buffer.allocate(mipLevelSize)
                        val offset1 = yIndex1.toLong() * it.width * it.width * it.channels
                        val offset2 = yIndex2.toLong() * it.width * it.width * it.channels
                        var readPtr1 = it.data.ptr + offset1
                        var readPtr2 = it.data.ptr + offset2
                        var writePtr = block.ptr
                        for (i in 0 until mipLevelSize) {
                            val v1 = readPtr1.getByte().toInt() and 0xFF
                            val v2 = readPtr2.getByte().toInt() and 0xFF
                            val v = (v1 * mul1 + v2 * mul2) / MIX_MUL
                            assert(v in 0..255)
                            writePtr.setByte(v.toByte())
                            readPtr1++
                            readPtr2++
                            writePtr++
                        }
                        PendingUpdateData(textureData, this, level, it.width, block)
                    }
            }
        }
    }

    companion object {
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        private const val MIX_MUL = 8388608
    }

    data class PendingUpdateData(
        private val dataSrc: MipmapTextureData,
        val sprite: TextureSprite,
        val level: Int,
        val imageSize: Int,
        val dataBufferBlock: PMappedRingBuffer.Block
    ) {
        val channels get() = dataSrc.channels
        val glFormat get() = dataSrc.glFormat
        val glDataType get() = dataSrc.glDataType
    }

    data class MipmapTextureData(
        val images: List<NativeImage>
    ) {
        val levels: Int; get() = images.size
        val width: Int; get() = images[0].width
        val height: Int; get() = images[0].height
        val channels: Int; get() = images[0].channels
        val glFormat: Int; get() = images[0].glFormat
        val glDataType: Int; get() = images[0].glDataType
    }
}
