package dev.luna5ama.fornax

import java.io.File
import java.io.IOException
import java.util.*

object FornaxMod {
    lateinit var instance: ModInstance; private set
    var config: Config = Config(); private set

    fun init() {
        if (::instance.isInitialized) {
            instance.destroy()
        }
        instance = ModInstance()

        try {
            File("fornax/config.properties").inputStream().use {
                val prop = Properties()
                prop.load(it)
                config = Config(
                    virtualTextureAtlasSize = prop.getProperty("virtualTextureAtlasSize", "16384").toInt()
                )
            }
        } catch (e: IOException) {
            // ignored
        }
    }

    fun shutdown() {
        instance.destroy()

        File("fornax/config.properties").outputStream().use {
            val prop = Properties()
            prop.setProperty("virtualTextureAtlasSize", config.virtualTextureAtlasSize.toString())
            prop.store(it, null)
        }
    }
}