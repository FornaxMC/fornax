package dev.luna5ama.fornax.terrain

import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.SparseBufferArena
import dev.luna5ama.fornax.opengl.register

class TerrainMeshManager : IGLObjContainer by IGLObjContainer.Impl() {
    val globalQuadBuffer = register(SparseBufferArena(128 * 1024 * 1024))
}