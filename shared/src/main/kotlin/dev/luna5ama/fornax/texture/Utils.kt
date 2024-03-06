package dev.luna5ama.fornax.texture

import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.ensureCapacity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.InputStream
import javax.imageio.ImageIO


private fun BufferedImage.createDataArray(): Any {
    val numBands = raster.numBands
    return when (val dataType = raster.dataBuffer.dataType) {
        DataBuffer.TYPE_BYTE -> ByteArray(numBands)
        DataBuffer.TYPE_USHORT -> ShortArray(numBands)
        DataBuffer.TYPE_INT -> IntArray(numBands)
        DataBuffer.TYPE_FLOAT -> FloatArray(numBands)
        DataBuffer.TYPE_DOUBLE -> DoubleArray(numBands)
        else -> throw IllegalArgumentException("Unknown data buffer type: $dataType")
    }
}

suspend fun BufferedImage.getRGBA(buffer: Arr): Arr {
    buffer.ensureCapacity(width * height * 4L, false)

    coroutineScope {
        var ptrM = buffer.ptr
        for (y in (0 until height).reversed()) {
            val ptrC = ptrM
            ptrM += width * 4L
            launch(Dispatchers.Default) {
                var ptr = ptrC

                val data = createDataArray()

                for (x in 0 until width) {
                    val dataElement = raster.getDataElements(x, y, data)

                    ptr = ptr.setByteInc(colorModel.getRed(dataElement).toByte())
                        .setByteInc(colorModel.getGreen(dataElement).toByte())
                        .setByteInc(colorModel.getBlue(dataElement).toByte())
                        .setByteInc(colorModel.getAlpha(dataElement).toByte())
                }
            }
        }
    }

    return buffer
}

suspend fun BufferedImage.getRG(buffer: Arr): Arr {
    buffer.ensureCapacity(width * height * 2L, false)

    coroutineScope {
        var ptrM = buffer.ptr
        for (y in (0 until height).reversed()) {
            val ptrC = ptrM
            ptrM += width * 2L
            launch(Dispatchers.Default) {
                val data = createDataArray()
                var ptr = ptrC
                for (x in 0 until width) {
                    val dataElement = raster.getDataElements(x, y, data)
                    ptr = ptr.setByteInc(colorModel.getRed(dataElement).toByte())
                        .setByteInc(colorModel.getGreen(dataElement).toByte())
                }
            }
        }
    }

    return buffer
}

suspend fun BufferedImage.getRGB(buffer: Arr): Arr {
    buffer.ensureCapacity(width * height * 3L, false)

    coroutineScope {
        var ptrM = buffer.ptr
        for (y in (0 until height).reversed()) {
            val ptrC = ptrM
            ptrM += width * 3L
            launch(Dispatchers.Default) {
                val data = createDataArray()
                var ptr = ptrC
                for (x in 0 until width) {
                    val dataElement = raster.getDataElements(x, y, data)
                    ptr = ptr.setByteInc(colorModel.getRed(dataElement).toByte())
                        .setByteInc(colorModel.getGreen(dataElement).toByte())
                        .setByteInc(colorModel.getBlue(dataElement).toByte())
                }
            }
        }
    }

    return buffer
}

suspend fun BufferedImage.getR(buffer: Arr): Arr {
    buffer.ensureCapacity(width * height * 1L, false)

    coroutineScope {
        var ptrM = buffer.ptr
        for (y in (0 until height).reversed()) {
            val ptrC = ptrM
            ptrM += width * 1L
            launch(Dispatchers.Default) {
                val data = createDataArray()
                var ptr = ptrC
                for (x in 0 until width) {
                    val dataElement = raster.getDataElements(x, y, data)
                    ptr = ptr.setByteInc(colorModel.getRed(dataElement).toByte())
                }
            }
        }
    }

    return buffer
}

class NativeImage(val data: Arr, val width: Int, val height: Int, val glFormat: Int, val channels: Int, val glDataType: Int) {
    fun free() {
        data.free()
    }

    fun half(): NativeImage {
        val newWidth =  width shr 1
        val newHeight = height shr 1
        val channelBytes = channels.toLong()
        val newData = Arr.malloc(newWidth * newHeight * channelBytes)

        when (channels) {
            1 -> {
                for (y in 0 until newHeight) {
                    for (x in 0 until newWidth) {
                        val r1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r = (r1 + r2 + r3 + r4) shr 2
                        newData.ptr.setByte((y * newWidth + x) * channelBytes, r.toByte())
                    }
                }
            }
            2 -> {
                for (y in 0 until newHeight) {
                    for (x in 0 until newWidth) {
                        val r1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r = (r1 + r2 + r3 + r4) shr 2
                        val g1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g = (g1 + g2 + g3 + g4) shr 2
                        newData.ptr.setByte((y * newWidth + x) * channelBytes, r.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 1, g.toByte())
                    }
                }
            }
            3 -> {
                for (y in 0 until newHeight) {
                    for (x in 0 until newWidth) {
                        val r1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r = (r1 + r2 + r3 + r4) shr 2
                        val g1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g = (g1 + g2 + g3 + g4) shr 2
                        val b1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 2).toInt() and 0xFF
                        val b2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 2).toInt() and 0xFF
                        val b3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 2).toInt() and 0xFF
                        val b4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 2).toInt() and 0xFF
                        val b = (b1 + b2 + b3 + b4) shr 2
                        newData.ptr.setByte((y * newWidth + x) * channelBytes, r.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 1, g.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 2, b.toByte())
                    }
                }
            }
            4 -> {
                for (y in 0 until newHeight) {
                    for (x in 0 until newWidth) {
                        val r1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes).toInt() and 0xFF
                        val r4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes).toInt() and 0xFF
                        val r = (r1 + r2 + r3 + r4) shr 2
                        val g1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 1).toInt() and 0xFF
                        val g4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 1).toInt() and 0xFF
                        val g = (g1 + g2 + g3 + g4) shr 2
                        val b1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 2).toInt() and 0xFF
                        val b2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 2).toInt() and 0xFF
                        val b3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 2).toInt() and 0xFF
                        val b4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 2).toInt() and 0xFF
                        val b = (b1 + b2 + b3 + b4) shr 2
                        val a1 = data.ptr.getByte((y * 2 * width + x * 2) * channelBytes + 3).toInt() and 0xFF
                        val a2 = data.ptr.getByte((y * 2 * width + x * 2 + 1) * channelBytes + 3).toInt() and 0xFF
                        val a3 = data.ptr.getByte(((y * 2 + 1) * width + x * 2) * channelBytes + 3).toInt() and 0xFF
                        val a4 = data.ptr.getByte(((y * 2 + 1) * width + x * 2 + 1) * channelBytes + 3).toInt() and 0xFF
                        val a = (a1 + a2 + a3 + a4) shr 2
                        newData.ptr.setByte((y * newWidth + x) * channelBytes, r.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 1, g.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 2, b.toByte())
                        newData.ptr.setByte((y * newWidth + x) * channelBytes + 3, a.toByte())
                    }
                }
            }
        }

        return NativeImage(newData, newWidth, newHeight, glFormat, channels, glDataType)
    }

    companion object {
        private suspend fun readPngImage(input: InputStream): BufferedImage {
            return withContext(Dispatchers.IO) { ImageIO.read(input) }
        }

        suspend fun read(input: InputStream, channels: Int = -1): NativeImage {
            return withContext(Dispatchers.Default) {
                val bufferedImage = readPngImage(input)
                val acutalChannels = if (channels == -1) bufferedImage.raster.numBands else channels
                val data = when (acutalChannels) {
                    1 -> bufferedImage.getR(Arr.malloc(bufferedImage.width * bufferedImage.height * 1L))
                    2 -> bufferedImage.getRG(Arr.malloc(bufferedImage.width * bufferedImage.height * 2L))
                    3 -> bufferedImage.getRGB(Arr.malloc(bufferedImage.width * bufferedImage.height * 3L))
                    4 -> bufferedImage.getRGBA(Arr.malloc(bufferedImage.width * bufferedImage.height * 4L))
                    else -> throw IllegalArgumentException("Invalid number of channels: $acutalChannels")
                }
                val format = when (acutalChannels) {
                    1 -> GL_RED
                    2 -> GL_RG
                    3 -> GL_RGB
                    4 -> GL_RGBA
                    else -> throw IllegalArgumentException("Invalid number of channels: $acutalChannels")
                }
                val nativeImage = NativeImage(data, bufferedImage.width, bufferedImage.height, format, acutalChannels, GL_UNSIGNED_BYTE)
                nativeImage
            }
        }
    }
}