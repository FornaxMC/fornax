package dev.luna5ama.fornax.opengl

import dev.luna5ama.glwrapper.impl.GLObjectType
import dev.luna5ama.glwrapper.impl.IGLObject
import dev.luna5ama.kmogus.MemoryStack
import dev.luna5ama.kmogus.ensureCapacity
import java.util.*

fun <T : IGLObject> IGLObjContainer.register(obj: T): T {
    return register0(obj)
}

fun <T : IGLObjContainer> IGLObjContainer.register(container: T): T {
    return register0(container)
}

interface IGLObjContainer {
    fun <T : IGLObject> register0(obj: T): T
    fun <T : IGLObjContainer> register0(container: T): T
    fun destroy()
    fun collectObjs(output: MutableMap<GLObjectType, MutableSet<IGLObject>>)
    fun clearObjs()

    class Impl : IGLObjContainer {
        private val objs = mutableListOf<IGLObject>()
        private val containers = mutableListOf<IGLObjContainer>()

        override fun <T : IGLObject> register0(obj: T): T {
            objs.add(obj)
            return obj
        }

        override fun <T : IGLObjContainer> register0(container: T): T {
            containers.add(container)
            return container
        }

        override fun collectObjs(output: MutableMap<GLObjectType, MutableSet<IGLObject>>) {
            objs.forEach {
                output.getOrPut(it.type, ::mutableSetOf).add(it)
            }
            containers.forEach {
                it.collectObjs(output)
            }
        }

        override fun clearObjs() {
            objs.clear()
        }

        override fun destroy() {
            val objs = mutableMapOf<GLObjectType, MutableSet<IGLObject>>()
            collectObjs(objs)
            objs.forEach { _, v ->
                v.forEach {
                    it.resetID()
                }
            }
            MemoryStack {
                val arr = malloc(4L)
                objs.forEach mapLoop@{ k, v ->
                    arr.ensureCapacity(v.size * 4L, false)
                    var ptr = arr.ptr
                    var validCount = 0
                    v.forEach {
                        if (it.id == 0) return@forEach
                        ptr = ptr.setIntInc(it.id)
                        validCount++
                    }
                    k.destroy(validCount, arr.ptr)
                }
            }
        }
    }
}