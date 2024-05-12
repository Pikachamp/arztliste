rootProject.name = "arztliste-dialog"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    versionCatalogs {
        create("compiler") {
            version("kotlin", "1.9.23")
            version("compose", "1.6.2")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlinx-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("compose", "org.jetbrains.compose").versionRef("compose")

            version("json", "1.6.3")
            library("json-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef("json")
        }

        create("views") {
            library("file-picker", "com.darkrockstudios", "mpfilepicker").version("3.1.0")
        }
    }
}
