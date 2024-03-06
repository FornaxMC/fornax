package dev.luna5ama.fornax

import kotlin.coroutines.CoroutineContext

interface IUpdateListener {
    suspend fun onPreTick() {}
    suspend fun onPreTickParallel(mainContext: CoroutineContext) {}
    suspend fun onPostTick() {}
    suspend fun onPostTickParallel(mainContext: CoroutineContext) {}
    suspend fun onPreRender() {}
    suspend fun onPreRenderParallel(mainContext: CoroutineContext) {}
    suspend fun onPostRender() {}
    suspend fun onPostRenderParallel(mainContext: CoroutineContext) {}
}