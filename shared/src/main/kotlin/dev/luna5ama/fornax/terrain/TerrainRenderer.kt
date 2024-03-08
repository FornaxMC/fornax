package dev.luna5ama.fornax.terrain

import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register

class TerrainRenderer : IGLObjContainer by IGLObjContainer.Impl() {
    val meshManager = register(TerrainMeshManager())
}