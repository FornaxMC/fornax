package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.*
import dev.luna5ama.fornax.terrain.TerrainRenderer
import dev.luna5ama.fornax.texture.TextureManager
import dev.luna5ama.glwrapper.api.GL_MAP_COHERENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_WRITE_BIT
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ModInstance(
    glContextInitializer: BackgroundGL.GLContextInitializer
) : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    val globalScope = CoroutineScope(Dispatchers.Default)
    val backgroundGL = BackgroundGL(glContextInitializer)
    val samplerManager = register(SamplerManager())
    val mainGPUFence = GPUFence()
    val backgroundGPUFence = GPUFence()
    val terrainRenderer = register(TerrainRenderer())
    val textureManager = register(TextureManager(this))
    val globalUploadBuffer =
        register(PersistentRingBuffer(30, GL_MAP_COHERENT_BIT or GL_MAP_WRITE_BIT))

    init {
        backgroundGL.scope.launch {
            while (true) {
                backgroundGPUFence.update()
                delay(50L)
            }
        }
    }

    override suspend fun onPreTick() {
        textureManager.onPreTick()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                textureManager.onPreTickParallel(mainContext)
            }
        }
    }

    override suspend fun onPostTick() {
        textureManager.onPostTick()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                textureManager.onPostTickParallel(mainContext)
            }
        }
    }

    override suspend fun onPreRender() {
        textureManager.onPreRender()
        globalUploadBuffer.tryUpdate()
        coroutineScope {
            val mainContext = this.coroutineContext
            launch(Dispatchers.Default) {
                textureManager.onPreRenderParallel(mainContext)
            }
        }
    }

    override suspend fun onPostRender() {
        mainGPUFence.update()
        textureManager.onPostRender()
        coroutineScope {
            val mainContext = this.coroutineContext
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