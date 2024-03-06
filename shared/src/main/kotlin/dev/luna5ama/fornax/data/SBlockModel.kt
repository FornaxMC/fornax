package dev.luna5ama.fornax.data

import kotlinx.serialization.Serializable

@Serializable
data class SBlockModel(
    val parent: ResourceReference? = null,
//    val gui_light: String? = null,
//    val display: Map<Display.Position, Display>? = null,
    val ambientocclusion: Boolean = true,
    val textures: Map<String, ResourceReference>? = null,
    val elements: List<Element>? = null
) {
    @Serializable
    data class Display(
        val rotation: SVec3,
        val translation: SVec3,
        val scale: SVec3
    ) {
        @Serializable
        enum class Position {
            thirdperson_righthand,
            thirdperson_lefthand,
            firstperson_righthand,
            firstperson_lefthand,
            gui,
            head,
            ground,
            fixed
        }
    }

    @Serializable
    data class Element(
//        val name: String? = null,
        val from: SVec3,
        val to: SVec3,
        val rotation: Rotation = Rotation.Default,
        val shade: Boolean = true,
        val faces: Map<Facing, Face>
    ) {

        @Serializable
        data class Rotation(
            val origin: SVec3,
            val axis: Axis,
            val angle: Float,
            val rescale: Boolean = false
        ) {
            companion object {
                val Default = Rotation(SVec3(8f, 8f, 8f), Axis.Y, 0f)
            }
        }

        @Serializable
        data class Face(
            val uv: SVec2? = null,
            val texture: String,
            val cullface: Facing? = null,
            val rotation: Int = 0,
            val tintindex: Int = -1
        )
    }
}