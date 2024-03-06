package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.IGLObjContainer
import dev.luna5ama.fornax.opengl.register
import dev.luna5ama.glwrapper.api.GL_NEAREST
import dev.luna5ama.glwrapper.api.GL_TEXTURE_MAG_FILTER
import dev.luna5ama.glwrapper.api.GL_TEXTURE_MIN_FILTER
import dev.luna5ama.glwrapper.impl.SamplerObject
import dev.luna5ama.glwrapper.impl.parameteri

class Samplers : IGLObjContainer by IGLObjContainer.Impl() {
    val nearest = register(SamplerObject())
        .parameteri(GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        .parameteri(GL_TEXTURE_MIN_FILTER, GL_NEAREST)
}