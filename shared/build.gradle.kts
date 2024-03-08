dependencies {
    val kmogusVersion: String by project
    val glWrapperVersion: String by project

    modCore("dev.fastmc:fastmc-common:1.1-SNAPSHOT") {
        isTransitive = false
    }

    modCore("dev.luna5ama:kmogus-core:$kmogusVersion")
    modCore("dev.luna5ama:kmogus-joml:$kmogusVersion")

    modCore("dev.luna5ama:gl-wrapper-core:$glWrapperVersion")
}