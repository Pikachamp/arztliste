import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization.compiler)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

group = "de.schott"
version = project.properties["version"] as String

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.kotlinx.serialization.json) {
        because("the input is a JSON file.")
    }
    implementation(compose.desktop.currentOs)
    implementation(libs.file.picker)
}

compose.desktop {
    application {
        mainClass = "de.schott.arztliste.MainDialogKt"

        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "arztliste"
            packageVersion = project.properties["version"] as String
            modules("jdk.unsupported")
            windows {
                shortcut = true
                menu = true
            }
        }
    }
}
