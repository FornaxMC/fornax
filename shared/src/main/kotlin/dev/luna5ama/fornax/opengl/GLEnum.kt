package dev.luna5ama.fornax.opengl

import dev.luna5ama.glwrapper.api.*

interface GLEnum {
    val value: Int
}

enum class WrapMode(override val value: Int) : GLEnum {
    CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
    CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER),
    MIRRORED_REPEAT(GL_MIRRORED_REPEAT),
    MIRROR_CLAMP_TO_EDGE(GL_MIRROR_CLAMP_TO_EDGE),
    REPEAT(GL_REPEAT)
}

sealed interface FilterMode : GLEnum {
    enum class Min(override val value: Int) : FilterMode {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL_NEAREST_MIPMAP_NEAREST),
        LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR)
    }

    enum class Mag(override val value: Int) : FilterMode {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR)
    }
}

enum class CompareMode(override val value: Int) : GLEnum {
    NONE(GL_NONE),
    COMPARE_REF_TO_TEXTURE(GL_COMPARE_REF_TO_TEXTURE)
}

enum class CompareFunc(override val value: Int) : GLEnum {
    NEVER(GL_NEVER),
    LESS(GL_LESS),
    EQUAL(GL_EQUAL),
    LEQUAL(GL_LEQUAL),
    GREATER(GL_GREATER),
    NOTEQUAL(GL_NOTEQUAL),
    GEQUAL(GL_GEQUAL),
    ALWAYS(GL_ALWAYS)
}