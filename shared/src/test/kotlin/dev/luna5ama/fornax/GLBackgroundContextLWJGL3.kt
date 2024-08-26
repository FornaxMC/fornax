package dev.luna5ama.fornax

import dev.luna5ama.fornax.opengl.BackgroundGL
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL

class GLContextInitializerLWJGL3 : BackgroundGL.GLContextInitializer {
    private val backgroundWindow = glfwCreateWindow(1, 1, "", 0, glfwGetCurrentContext())

    init {
        glfwHideWindow(backgroundWindow)
    }

    override fun initGLContext() {
        glfwMakeContextCurrent(backgroundWindow)
        GL.createCapabilities()
    }
}