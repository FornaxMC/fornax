package dev.luna5ama.fornax.texture

import dev.fastmc.common.TickTimer
import dev.luna5ama.fornax.IUpdateListener
import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.util.sendTo
import dev.luna5ama.glwrapper.enums.ImageFormat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class TextureManager(val mod: ModInstance) : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    val atlas = register(VirtualTextureAtlas(ImageFormat.R8_G8_B8_A8_UN))
    private val sprites0 = ConcurrentHashMap<ResourceReference, TextureSprite>()
    val sprites: Map<ResourceReference, TextureSprite> get() = sprites0
    private val animatedSprites = CopyOnWriteArrayList<TextureSprite>()
    private var tickCounter = 0L
    private val printTimer = TickTimer()
    private val updateCounter = AtomicInteger()

    private val pendingUpdates = Channel<TextureSprite.PendingUpdateData>(Channel.UNLIMITED)
    var updateAnimation = false

    fun registerSprite(ref: ResourceReference): Deferred<TextureSprite> {
        val counter = tickCounter
        return mod.globalScope.async {
            sprites0.computeIfAbsent(ref) {
                val sprite = TextureSprite(it)
                if (sprite.animationMeta != null) {
                    animatedSprites.add(sprite)
                }
                launch {
                    sprite.getFrame(mod.globalUploadBuffer, counter).sendTo(pendingUpdates)
                }
                sprite
            }
        }
    }

    override suspend fun onPostTickParallel(mainContext: CoroutineContext) {
        if (!updateAnimation) return
        val counter = tickCounter++
        animatedSprites.forEach { sprite ->
            mod.globalScope.launch {
                sprite.getFrame(mod.globalUploadBuffer, counter).sendTo(pendingUpdates)
            }
        }
    }

    override suspend fun onPreRender() {
        var update = pendingUpdates.tryReceive().getOrNull()
        while (update != null) {
            var atlasBlock = update.sprite.getAtlasBlock(update.level)
            if (atlasBlock == null || atlasBlock.size < update.imageSize) {
                atlasBlock?.free()
                atlasBlock = atlas.allocate(update.imageSize) ?: error("Failed to allocate atlas block")
                update.sprite.registerAtlasBlock(update.level, atlasBlock)
            }
            atlasBlock.invalidate()
            atlasBlock.upload(
                update.glFormat,
                update.glDataType,
                update.dataBufferBlock.bufferObject,
                update.dataBufferBlock.offset
            )
            val dataBufferBlock = update.dataBufferBlock
            mod.globalScope.launch {
                mod.mainGPUFence.awaitGPU()
                dataBufferBlock.free()
            }
            update = pendingUpdates.tryReceive().getOrNull()
            updateCounter.incrementAndGet()
        }
        if (printTimer.tickAndReset(3000)) {
            println("Updates: %,d".format(updateCounter.getAndSet(0)))
        }
    }
}