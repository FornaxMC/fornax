package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.terrain.TerrainRenderer

class ModInstance : IGLObjContainer by IGLObjContainer.Impl() {
    val terrainRenderer = register(TerrainRenderer())
}