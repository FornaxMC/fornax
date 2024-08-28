package dev.luna5ama.fornax

import dev.fastmc.common.TickTimer
import dev.luna5ama.fornax.data.ResourceReference
import dev.luna5ama.glwrapper.ShaderProgram
import dev.luna5ama.glwrapper.ShaderSource
import dev.luna5ama.glwrapper.api.*
import dev.luna5ama.glwrapper.enums.FilterMode
import dev.luna5ama.glwrapper.objects.VertexArrayObject
import kotlinx.coroutines.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import java.io.File

object AtlasTest {
    @JvmStatic
    fun main(args: Array<String>) {
        glfwInit()

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1)
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, 1)
        glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_TRUE)

        val window = glfwCreateWindow(1024, 1024, "OpenGL", 0, 0)
//        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE)

        glfwMakeContextCurrent(window)
        GL.createCapabilities()

        glfwSwapInterval(0)

        println("A")
        val instance = Instance()
        println("B")

        runBlocking {
            while (!glfwWindowShouldClose(window)) {
                glfwPollEvents()
                instance.render()
                glfwSwapBuffers(window)
            }
        }

        glfwTerminate()
    }

    class Instance {
        private val modInstance = ModInstance(GLContextInitializerLWJGL3())
        private val shader = ShaderProgram(
            ShaderSource.Vert("Blit.vert"),
            ShaderSource.Frag("Blit.frag")
        )
        private val dummyVao = VertexArrayObject().apply {
            create()
        }

        private val timer = TickTimer()
        private val gcTimer = TickTimer()

        private val handle: Long

        init {

            handle = glGetTextureSamplerHandleARB(
                modInstance.textureManager.atlas.textureObject.id,
                modInstance.samplerManager.get {
                    minFilter(FilterMode.Nearest)
                    magFilter(FilterMode.Nearest)
                }.id
            )
            glMakeTextureHandleResidentARB(handle)
        }

        private val stuff =
            //            File("test\\AVPBR Retexture R2\\assets\\minecraft\\textures\\block").listFiles()!!
            File("test\\SPBR-15_1\\assets\\minecraft\\textures\\block").listFiles()!!
                .asSequence()
                .filter { it.extension == "png" }
                .map { ResourceReference("textures/block/${it.nameWithoutExtension}") }
                .map {
                    modInstance.textureManager.registerSprite(it)
                }
                .toList()

        init {
            modInstance.baseCoroutineScope.launch {
                stuff.awaitAll()
                modInstance.textureManager.updateAnimation = true
            }
        }

        suspend fun render() {
            coroutineScope {
                if (timer.tickAndReset(50)) {
                    modInstance.onPreTick()
                    modInstance.onPostTick()
                }

                modInstance.onPreRender()

                glBindFramebuffer(GL_FRAMEBUFFER, 0)

                glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                glClearDepthf(1.0f)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                glViewport(0, 0, 1024, 1024)

                glDisable(GL_DEPTH_TEST)
                glDisable(GL_CULL_FACE)
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                shader.uniformHandle("uTex1", handle)
                dummyVao.bind()
                shader.bind()
                glDrawArrays(GL_TRIANGLES, 0, 6)

                modInstance.onPostRender()
            }
        }
    }
}