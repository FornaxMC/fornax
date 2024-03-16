
package dev.luna5ama.fornax

import dev.luna5ama.fornax.data.STexture
import kotlinx.serialization.json.Json
import java.io.File

object BlockModelParserTest {
    @JvmStatic
    fun main(args: Array<String>) {
        File("test\\vanilla\\textures\\block").listFiles()!!.forEach {
            if (it.extension != "mcmeta") return@forEach
            try {
                val jsonParser = Json {
                    ignoreUnknownKeys = true
                }
                val model = jsonParser.decodeFromString<STexture>(it.readText())
                println(model)
            } catch (e: Exception) {
                println(it)
                e.printStackTrace()
                return
            }
        }
    }
}