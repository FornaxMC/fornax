package dev.luna5ama.gltest.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
class DrawElementsIndirectCommand(
    val count: Int,
    val instanceCount: Int,
    val firstIndex: Int,
    val baseVertex: Int,
    val baseInstance: Int
)