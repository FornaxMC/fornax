import dev.fastmc.modsetup.minecraftVersion

architecturyProject {
    modPackage.set("dev.luna5ama.fornax")
    mixinConfig(
        "mixins.fornax-core.json",
        "mixins.fornax-accessor.json"
    )
    accessWidenerPath.set(file("common/src/main/resources/fornax.accesswidener").absoluteFile)

    commonProject {
        dependencies {
            val glWrapperVersion: String by project
            modCore("dev.luna5ama:gl-wrapper-lwjgl-3:$glWrapperVersion")
            modCore("dev.luna5ama:gl-wrapper-${project.minecraftVersion}:$glWrapperVersion")
        }
    }
}