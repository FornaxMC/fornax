package dev.luna5ama.gltest.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
class FontVertex(
    val position: Vec2f32,
    val vertUV: Vec2i16,
    val colorIndex: Byte,
    val overrideColor: Byte,
    val shadow: Byte
)