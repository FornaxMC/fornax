rootProject.name = "fornax"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.luna5ama.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        mavenLocal()
    }

    val kmogusVersion: String by settings

    plugins {
        id("dev.luna5ama.kmogus-struct-plugin") version kmogusVersion
    }
}

include("shared")
//include("forge-1.12.2")
//include("architectury-1.16.5", "architectury-1.16.5:common", "architectury-1.16.5:fabric", "architectury-1.16.5:forge")
//include("architectury-1.18.2", "architectury-1.18.2:common", "architectury-1.18.2:fabric", "architectury-1.18.2:forge")
//include("architectury-1.19.4", "architectury-1.19.4:common", "architectury-1.19.4:fabric", "architectury-1.19.4:forge")
include("architectury-1.19.4", "architectury-1.19.4:common", "architectury-1.19.4:fabric")
