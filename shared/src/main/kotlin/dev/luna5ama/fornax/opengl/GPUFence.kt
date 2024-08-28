package dev.luna5ama.fornax.opengl

import dev.luna5ama.glwrapper.api.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GPUFence {
    private val lock = ReentrantReadWriteLock()
    private val frames = ArrayDeque<Frame>()

    @Volatile
    private var currentFrame0 = Frame()

    internal fun update() {
        val last = lock.write {
            val last = currentFrame0
            currentFrame0 = Frame()
            last
        }

        last.glSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)

        while (frames.isNotEmpty()) {
            val frame = frames.peekFirst()
            if (frame.isDone || frame.glSync == 0L || glGetSynci(frame.glSync, GL_SYNC_STATUS) == GL_SIGNALED) {
                if (frame.glSync != 0L) {
                    glDeleteSync(frame.glSync)
                }
                frame.isDone = true
                frame.glSync = 0L
                frame.continuations.forEach {
                    it.resumeWith(resumeResult)
                }
                assert(frames.removeFirst() === frame)
            } else {
                break
            }
        }

        frames.addLast(last)
    }

    suspend fun awaitGPU() {
        suspendCoroutine {
            lock.read {
                currentFrame0.continuations.add(it)
            }
        }
    }

    private companion object {
        val resumeResult = Result.success(Unit)
    }

    private class Frame {
        var glSync = 0L
        @Volatile
        var isDone = false

        val continuations = ConcurrentLinkedQueue<Continuation<Unit>>()
    }
}