package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.*
import dev.luna5ama.fornax.terrain.TerrainRenderer
import dev.luna5ama.fornax.texture.TextureManager
import dev.luna5ama.glwrapper.api.GL_MAP_COHERENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_WRITE_BIT
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ModInstance : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    val globalScope = CoroutineScope(Dispatchers.Default)
    val samplerManager = register(SamplerManager())
    val frameStamps = FrameStamps()
    val terrainRenderer = register(TerrainRenderer())
    val textureManager = register(TextureManager(this))
    val globalUploadBuffer =
        register(PMappedRingBuffer(1024L * 1024 * 1024 * 2, frameStamps, GL_MAP_COHERENT_BIT or GL_MAP_WRITE_BIT))

    override suspend fun onPreTick() {
        frameStamps.onPreTick()
        textureManager.onPreTick()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                frameStamps.onPreTickParallel(mainContext)
                textureManager.onPreTickParallel(mainContext)
            }
        }
    }

    override suspend fun onPostTick() {
        frameStamps.onPostTick()
        textureManager.onPostTick()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                frameStamps.onPostTickParallel(mainContext)
            }
            launch(Dispatchers.Default) {
                textureManager.onPostTickParallel(mainContext)
            }
        }
    }

    override suspend fun onPreRender() {
        frameStamps.onPreRender()
        textureManager.onPreRender()
        globalUploadBuffer.update()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                frameStamps.onPreRenderParallel(mainContext)
            }
            launch(Dispatchers.Default) {
                textureManager.onPreRenderParallel(mainContext)
            }
        }
    }

    override suspend fun onPostRender() {
        frameStamps.onPostRender()
        textureManager.onPostRender()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                frameStamps.onPostRenderParallel(mainContext)
            }
            launch(Dispatchers.Default) {
                textureManager.onPostRenderParallel(mainContext)
            }
        }
    }

    override suspend fun onPreTickParallel(mainContext: CoroutineContext) {
        throw UnsupportedOperationException("ROOT")
    }

    override suspend fun onPostTickParallel(mainContext: CoroutineContext) {
        throw UnsupportedOperationException("ROOT")
    }

    override suspend fun onPreRenderParallel(mainContext: CoroutineContext) {
        throw UnsupportedOperationException("ROOT")
    }

    override suspend fun onPostRenderParallel(mainContext: CoroutineContext) {
        throw UnsupportedOperationException("ROOT")
    }
}