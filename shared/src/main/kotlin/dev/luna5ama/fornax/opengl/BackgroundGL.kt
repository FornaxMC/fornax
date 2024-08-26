package dev.luna5ama.fornax.opengl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BackgroundGL(glContextInitializer: GLContextInitializer) {
    val executor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        ThreadFactory { Thread(it, "BackgroundGL") }
    )
    val context = executor.asCoroutineDispatcher()
    val scope = CoroutineScope(context)

    init {
        executor.submit(glContextInitializer::initGLContext).get()
    }

    interface GLContextInitializer {
        fun initGLContext()
    }
}