package dev.luna5ama.fornax.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ThreadPoolExecutor
import kotlin.coroutines.CoroutineContext

class CustomCoroutineScope(parentContext: CoroutineContext, val executor: ThreadPoolExecutor) :
    CoroutineScope by CoroutineScope(parentContext + executor.asCoroutineDispatcher())