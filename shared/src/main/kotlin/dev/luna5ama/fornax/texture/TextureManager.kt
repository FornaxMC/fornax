package dev.luna5ama.fornax.texture

import dev.fastmc.common.TickTimer
import dev.luna5ama.fornax.IUpdateListener
import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.glwrapper.enums.ImageFormat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    processUpdate(sprite.getFrame(mod.globalUploadBuffer, counter))
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
                processUpdate(sprite.getFrame(mod.globalUploadBuffer, counter))
            }
        }
    }

    private suspend fun processUpdate(flow: Flow<TextureSprite.PendingUpdateData>) {
        withContext(mod.backgroundGL.scope.coroutineContext) {
            flow.collect { update ->
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
                updateCounter.incrementAndGet()
                mod.backgroundGPUFence.awaitGPU()
                update.dataBufferBlock.free()
            }
        }
    }

    override suspend fun onPreRender() {
        if (printTimer.tickAndReset(3000)) {
            println("Updates: %,d".format(updateCounter.getAndSet(0)))
        }
    }
}