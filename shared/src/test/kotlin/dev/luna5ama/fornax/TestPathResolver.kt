package dev.luna5ama.fornax

import dev.luna5ama.fornax.data.PathResolver
import dev.luna5ama.fornax.data.ResourceReference
import java.io.File
import java.net.URI


class TestPathResolver : PathResolver {
    override val priority: Int
        get() = 1

    override fun resolve(ref: ResourceReference): URI? {
        return File("test\\AVPBR Retexture R2\\assets\\minecraft\\${ref.path}").toURI()
    }

}