package dev.luna5ama.fornax.terrain

import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.fornax.texture.VirtualTextureAtlas
import dev.luna5ama.glwrapper.api.GL_RGBA8

class TerrainRenderer : IGLObjContainer by IGLObjContainer.Impl() {
    val meshManager = register(TerrainMeshManager())
    val textureAtlas = register(VirtualTextureAtlas(GL_RGBA8))
}