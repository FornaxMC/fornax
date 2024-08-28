package dev.luna5ama.fornax.texture

import dev.fastmc.common.TickTimer
import dev.luna5ama.fornax.IUpdateListener
import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.PersistentRingBuffer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.glwrapper.enums.ImageFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class TextureManager(val mod: ModInstance) : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    private val coroutineScope =
        CoroutineScope(mod.baseCoroutineScope.coroutineContext + CoroutineName("TextureManager"))
    val atlas = register(VirtualTextureAtlas(ImageFormat.R8_G8_B8_A8_UN))
    private val sprites0 = ConcurrentHashMap<ResourceReference, TextureSprite>()
    val sprites: Map<ResourceReference, TextureSprite> get() = sprites0
    private var tickCounter = 0L
    private val printTimer = TickTimer()
    private val updateCounter = AtomicInteger()
    private val animationUpdates = MutableSharedFlow<Long>(replay = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var updateAnimation = false

    fun registerSprite(ref: ResourceReference): Deferred<TextureSprite> {
        val counter = tickCounter
        return coroutineScope.async {
            sprites0.computeIfAbsent(ref) {
                val sprite = TextureSprite(it)
                if (sprite.animationMeta != null) {
                    animationUpdates.onEach { tick2 ->
                        processUpdate(sprite.getFrame(mod.globalUploadBuffer, tick2))
                    }.launchIn(coroutineScope)
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
        animationUpdates.tryEmit(counter)
    }

    private suspend fun processUpdate(flow: Flow<TextureSprite.PendingUpdateData>) {
            val blocks = mutableListOf<PersistentRingBuffer.Block>()
            withContext(mod.backgroundGL.coroutineScope.coroutineContext) {
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
                    blocks.add(update.dataBufferBlock)
                coroutineScope.launch {
                    mod.backgroundGL.gpuFence.awaitGPU()
                    update.dataBufferBlock.free()
                }
                }
            }
            updateCounter.incrementAndGet()
    }

    override suspend fun onPreRender() {
        if (printTimer.tickAndReset(3000)) {
            println("Updates: %,d".format(updateCounter.getAndSet(0) / 3 / animationUpdates.subscriptionCount.value))
        }
    }
}