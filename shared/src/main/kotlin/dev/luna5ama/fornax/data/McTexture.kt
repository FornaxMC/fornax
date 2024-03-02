package dev.luna5ama.fornax.data

import kotlinx.serialization.Serializable

@Serializable
data class STexture(
    val animation: SAnimation? = null,
)

@Serializable
data class SAnimation(
    val interpolate: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val frametime: Int = 1,
    val frames: List<Int>? = null,
)