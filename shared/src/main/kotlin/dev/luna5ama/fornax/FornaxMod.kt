package dev.luna5ama.fornax

object FornaxMod {
    lateinit var instance: ModInstance; private set

    fun init() {
        if (::instance.isInitialized) {
            instance.destroy()
        }
        instance = ModInstance()
    }
}