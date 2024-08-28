package dev.luna5ama.fornax.opengl

import dev.luna5ama.fornax.ModInstance
import dev.luna5ama.fornax.util.CustomCoroutineScope
import kotlinx.coroutines.CoroutineName
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory

class BackgroundGL(mod: ModInstance, private val glContextInitializer: GLContextInitializer) {
    val coroutineScope = CustomCoroutineScope(
        mod.baseCoroutineScope.coroutineContext + CoroutineName("BackgroundGL"),
        ScheduledThreadPoolExecutor(
            1,
            ThreadFactory { Thread(initializeThread(it), "BackgroundGL").apply { priority = Thread.MAX_PRIORITY } }
        )
    )
    val gpuFence = GPUFence()

    private fun initializeThread(runnable: Runnable): Runnable {
        return Runnable {
            glContextInitializer.initGLContext()
            runnable.run()
        }
    }

    interface GLContextInitializer {
        fun initGLContext()
    }
}