import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

allprojects {
    group = "dev.fastmc"
    version = "0.0.3"
}

runVmOptions {
    add(
        "-Xms2G",
        "-Xmx2G",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+AlwaysPreTouch",
        "-XX:+UseStringDeduplication",
        "-XX:+UseLargePages",
        "-XX:MaxGCPauseMillis=5",
        "-Djoml.forceUnsafe=true",
        "-Djoml.fastmath=true",
        "-Djoml.sinLookup=true",
        "-Djoml.format=false",
        "-Djoml.useMathFma=true",
    )
}

plugins {
    id("dev.fastmc.mod-setup").version("1.3.1")
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.luna5ama.dev/")
        maven("https://libraries.minecraft.net/")
        mavenLocal()
    }

    dependencies {
        val kotlinVersion: String by rootProject
        val kotlinxCoroutineVersion: String by rootProject
        val jomlVersion: String by rootProject

        testImplementation(kotlin("test"))

        "libraryImplementation"(kotlin("stdlib-jdk8", kotlinVersion))
        "libraryImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion")
        "libraryImplementation"("org.joml:joml:$jomlVersion")

        compileOnly("org.apache.logging.log4j:log4j-api:2.8.1")
        compileOnly("it.unimi.dsi:fastutil:7.1.0")
    }

    tasks {
        test {
            useJUnitPlatform()
            jvmArgs("-Xmx2G")
        }
        withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
                    "-Xbackend-threads=0"
                )
            }
        }
    }
}

tasks {
    val collectJars by register<Copy>("collectJars") {
        group = "build"

        from(
            provider {
                subprojects.mapNotNull { it.tasks.findByName("modLoaderJar")?.outputs }
            }
        )

        into(file("${layout.buildDirectory}/libs"))
    }

    assemble {
        finalizedBy(collectJars)
    }
}