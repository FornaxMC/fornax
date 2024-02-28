@file:OptIn(ExperimentalSerializationApi::class)

package dev.luna5ama.fornax.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SVec2.Serializer::class)
data class SVec2(
    val x: Float,
    val y: Float
) {
    object Serializer : KSerializer<SVec2> {
        override val descriptor: SerialDescriptor = listSerialDescriptor<Float>()

        override fun serialize(encoder: Encoder, value: SVec2) {
            encoder.encodeSerializableValue(ListSerializer(Float.serializer()), listOf(value.x, value.y))
        }

        override fun deserialize(decoder: Decoder): SVec2 {
            val list = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
            return SVec2(list[0], list[1])
        }
    }
}

@Serializable(with = SVec3.Serializer::class)
data class SVec3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    object Serializer : KSerializer<SVec3> {
        override val descriptor: SerialDescriptor = listSerialDescriptor<Float>()

        override fun serialize(encoder: Encoder, value: SVec3) {
            encoder.encodeSerializableValue(ListSerializer(Float.serializer()), listOf(value.x, value.y, value.z))
        }

        override fun deserialize(decoder: Decoder): SVec3 {
            val list = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
            return SVec3(list[0], list[1], list[2])
        }
    }
}

@Serializable(with = SVec4.Serializer::class)
data class SVec4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    object Serializer : KSerializer<SVec4> {
        override val descriptor: SerialDescriptor = listSerialDescriptor<Float>()

        override fun serialize(encoder: Encoder, value: SVec4) {
            encoder.encodeSerializableValue(
                ListSerializer(Float.serializer()),
                listOf(value.x, value.y, value.z, value.w)
            )
        }

        override fun deserialize(decoder: Decoder): SVec4 {
            val list = decoder.decodeSerializableValue(ListSerializer(Float.serializer()))
            return SVec4(list[0], list[1], list[2], list[3])
        }
    }
}

@Serializable(with = ResourceRefernce.Serializer::class)
data class ResourceRefernce(
    val namespace: String = "minecraft",
    val path: String
) {
    object Serializer : KSerializer<ResourceRefernce> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ResourceRefernce", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ResourceRefernce) {
            encoder.encodeString("${value.namespace}:${value.path}")
        }

        override fun deserialize(decoder: Decoder): ResourceRefernce {
            val split = decoder.decodeString().split(":")
            return if (split.size == 1) {
                ResourceRefernce(path = split[0])
            } else {
                ResourceRefernce(split[0], split[1])
            }
        }
    }
}

@Serializable(with = Facing.Serializer::class)
enum class Facing {
    DOWN,
    UP,
    NORTH,
    SOUTH,
    WEST,
    EAST;

    val lowercaseName = name.lowercase()

    companion object {
        fun fromLowercaseName(name: String): Facing {
            return when (name) {
                "down" -> DOWN
                "up" -> UP
                "north" -> NORTH
                "south" -> SOUTH
                "west" -> WEST
                "east" -> EAST
                "bottom" -> DOWN
                "top" -> UP
                else -> throw IllegalArgumentException("Invalid face name: $name")
            }
        }
    }

    object Serializer : KSerializer<Facing> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Facing", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Facing) {
            encoder.encodeString(value.lowercaseName)
        }

        override fun deserialize(decoder: Decoder): Facing {
            return Facing.fromLowercaseName(decoder.decodeString())
        }
    }
}

@Serializable(with = Axis.Serializer::class)
enum class Axis {
    X,
    Y,
    Z;

    val lowercaseName = name.lowercase()

    companion object {
        fun fromLowercaseName(name: String): Axis {
            return when (name) {
                "x" -> X
                "y" -> Y
                "z" -> Z
                else -> throw IllegalArgumentException("Invalid axis name: $name")
            }
        }
    }

    object Serializer : KSerializer<Axis> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Axis", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Axis) {
            encoder.encodeString(value.lowercaseName)
        }

        override fun deserialize(decoder: Decoder): Axis {
            return Axis.fromLowercaseName(decoder.decodeString())
        }
    }
}