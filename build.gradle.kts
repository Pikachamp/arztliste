import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.23"
    alias(compiler.plugins.kotlinx.serialization)
    id("org.jetbrains.compose") version "1.6.2"
}

group = "de.schott"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}


dependencies {
    implementation(compiler.json.serialization) {
        because("the input is a JSON file.")
    }
    implementation(compose.desktop.currentOs)
    implementation(views.file.picker)
}

compose.desktop {
    application {
        mainClass = "de.schott.arztliste.MainDialogKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "arztliste"
            packageVersion = "1.0.0"
            modules("jdk.unsupported")
            windows {
                shortcut = true
                menu = true
            }
        }
    }
}
