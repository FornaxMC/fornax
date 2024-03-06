package dev.luna5ama.fornax.opengl

import dev.luna5ama.fornax.IUpdateListener
import dev.luna5ama.glwrapper.api.*
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FrameStamps : IUpdateListener {
    private val lock = ReentrantReadWriteLock()
    private val stamps = ArrayDeque<StampImpl>()

    @Volatile
    private var currentStamp0 = StampImpl()

    val currentStamp: Stamp get() = lock.read { currentStamp0 }

    override suspend fun onPreRender() {
        update()
    }

    override suspend fun onPostRender() {
        update()
    }

    fun update() {
        val last = lock.write {
            val last = currentStamp0
            currentStamp0 = StampImpl()
            last
        }

        last.glSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)

        while (stamps.isNotEmpty()) {
            val stamp = stamps.peekFirst()
            if (stamp.isDone || stamp.glSync == 0L || glGetSynci(stamp.glSync, GL_SYNC_STATUS) == GL_SIGNALED) {
                if (stamp.glSync != 0L) {
                    glDeleteSync(stamp.glSync)
                }
                stamp.isDone = true
                stamp.glSync = 0L
                stamps.removeFirst()
            } else {
                break
            }
        }

        stamps.addLast(last)
    }

    interface Stamp {
        val isDone: Boolean
    }

    private class StampImpl : Stamp {
        override var isDone = false
        var glSync = 0L
    }
}

val FrameStamps.Stamp?.isNullOrDone: Boolean get() = this?.isDone ?: true