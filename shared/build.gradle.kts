dependencies {
    val kmogusVersion: String by project
    val glWrapperVersion: String by project

    modCore("dev.fastmc:fastmc-common:1.1-SNAPSHOT") {
        isTransitive = false
    }

    modCore("dev.luna5ama:kmogus-core:$kmogusVersion")
    modCore("dev.luna5ama:kmogus-joml:$kmogusVersion")

    modCore("dev.luna5ama:gl-wrapper-core:$glWrapperVersion")

    testImplementation("dev.luna5ama:gl-wrapper-lwjgl-3:$glWrapperVersion")

    testImplementation(platform("org.lwjgl:lwjgl-bom:3.3.3"))
    testImplementation("org.lwjgl", "lwjgl")
    testImplementation("org.lwjgl", "lwjgl-glfw")
    testImplementation("org.lwjgl", "lwjgl-opengl")
    testImplementation("org.lwjgl", "lwjgl-stb")
    testRuntimeOnly("org.lwjgl", "lwjgl", classifier = "natives-windows")
    testRuntimeOnly("org.lwjgl", "lwjgl-glfw", classifier = "natives-windows")
    testRuntimeOnly("org.lwjgl", "lwjgl-opengl", classifier = "natives-windows")
    testRuntimeOnly("org.lwjgl", "lwjgl-stb", classifier = "natives-windows")
}