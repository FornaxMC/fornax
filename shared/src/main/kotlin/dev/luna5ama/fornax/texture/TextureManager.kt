package dev.luna5ama.fornax.texture

import dev.fastmc.common.pollEach
import dev.luna5ama.fornax.IUpdateListener
import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.util.sendTo
import dev.luna5ama.glwrapper.api.GL_RGBA8
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

class TextureManager(val mod: ModInstance) : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    val atlas = register(VirtualTextureAtlas(GL_RGBA8))
    private val sprites0 = ConcurrentHashMap<ResourceReference, TextureSprite>()
    val sprites: Map<ResourceReference, TextureSprite> get() = sprites0
    private val animatedSprites = CopyOnWriteArrayList<TextureSprite>()
    private val pendingSprites = ConcurrentLinkedQueue<ResourceReference>()
    private var tickCounter = 0L

    private val pendingUpdates = Channel<TextureSprite.PendingUpdateData>(Channel.UNLIMITED)

    fun registerSprite(ref: ResourceReference) {
        pendingSprites.add(ref)
    }

    override suspend fun onPostTickParallel(mainContext: CoroutineContext) {
        val counter = tickCounter++
        animatedSprites.forEach { sprite ->
            mod.globalScope.launch {
                sprite.getFrame(mod.globalUploadBuffer, counter)
                    .sendTo(pendingUpdates)
            }
        }
    }

    override suspend fun onPreRenderParallel(mainContext: CoroutineContext) {
        val counter = tickCounter
        mod.globalScope.launch {
            pendingSprites.pollEach { ref ->
                sprites0.computeIfAbsent(ref) {
                    val sprite = TextureSprite(it)
                    if (sprite.animationMeta != null) {
                        animatedSprites.add(sprite)
                    }
                    launch {
                        sprite.getFrame(mod.globalUploadBuffer, counter)
                            .sendTo(pendingUpdates)
                    }
                    sprite
                }
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
            update = pendingUpdates.tryReceive().getOrNull()
        }
    }
}