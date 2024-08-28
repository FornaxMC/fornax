package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.*
import dev.luna5ama.fornax.terrain.TerrainRenderer
import dev.luna5ama.fornax.texture.TextureManager
import dev.luna5ama.fornax.util.CustomCoroutineScope
import dev.luna5ama.glwrapper.api.GL_MAP_COHERENT_BIT
import dev.luna5ama.glwrapper.api.GL_MAP_WRITE_BIT
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

class ModInstance(
    glContextInitializer: BackgroundGL.GLContextInitializer
) : IGLObjContainer by IGLObjContainer.Impl(), IUpdateListener {
    val baseCoroutineScope = CoroutineScope(CoroutineName("Fornax") + Dispatchers.Default)
    val timedLoopScope = run {
        val nThread = max(Runtime.getRuntime().availableProcessors() / 2, 1)
        val counter = AtomicInteger(0)
        CustomCoroutineScope(baseCoroutineScope.coroutineContext + CoroutineName("TimedLoop"), ThreadPoolExecutor(
            nThread,
            nThread,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactory { Thread(it, "TimedLoop#${counter.incrementAndGet()}") }
        ))
    }
    val backgroundGL = BackgroundGL(this, glContextInitializer)
    val samplerManager = register(SamplerManager())
    val mainGPUFence = GPUFence()
    val terrainRenderer = register(TerrainRenderer())
    val textureManager = register(TextureManager(this))
    val globalUploadBuffer =
        register(PersistentRingBuffer(30, GL_MAP_COHERENT_BIT or GL_MAP_WRITE_BIT))

    init {
        timedLoopScope.launch(backgroundGL.coroutineScope.coroutineContext) {
            while (true) {
                backgroundGL.gpuFence.update()
                delay(50L)
            }
        }
        timedLoopScope.launch {
            while (true) {
                globalUploadBuffer.update()
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