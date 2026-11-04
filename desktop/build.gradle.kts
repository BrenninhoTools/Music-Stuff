import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    sourceSets.named("main") {
        kotlin.srcDir("../app/src/main/java")
        kotlin.exclude(
            "com/brenninhotools/musicstuff/MainActivity.kt",
            "com/brenninhotools/musicstuff/android/**",
        )
        resources.srcDir("../app/src/main/res/drawable")
        resources.srcDir("../app/src/main/res/font")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.brenninhotools.musicstuff.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "Music Stuff"
            packageVersion = "0.0.2"
            description = "Convert FL Studio projects for FL Studio Mobile"
            vendor = "BrenninhoTools"
            modules("jdk.unsupported")
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "Music Stuff"
                shortcut = true
                menu = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "2c8f1a57-93de-4b60-8e14-7d05c6ab31f2"
            }
        }
    }
}
