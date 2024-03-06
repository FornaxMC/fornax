package dev.luna5ama.fornax.data

import kotlinx.serialization.Serializable

@Serializable
data class STexture(
    val animation: Animation? = null,
) {
    @Serializable
    data class Animation(
        val interpolate: Boolean = false,
        val width: Int? = null,
        val height: Int? = null,
        val frametime: Int = 1,
        val frames: List<Int>? = null,
    )
}