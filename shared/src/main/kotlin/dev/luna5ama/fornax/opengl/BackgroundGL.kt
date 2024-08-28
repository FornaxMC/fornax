package dev.luna5ama.fornax.opengl

import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.util.CustomCoroutineScope
import kotlinx.coroutines.CoroutineName
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BackgroundGL(mod: ModInstance, glContextInitializer: GLContextInitializer) {
    val coroutineScope = CustomCoroutineScope(
        mod.baseCoroutineScope.coroutineContext + CoroutineName("BackgroundGL"),
        ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactory { Thread(it, "BackgroundGL") }
        )
    )
    val gpuFence = GPUFence()

    init {
        coroutineScope.executor.submit(glContextInitializer::initGLContext).get()
    }

    interface GLContextInitializer {
        fun initGLContext()
    }
}