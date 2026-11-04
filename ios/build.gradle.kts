plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "MusicStuff"
            isStatic = true
        }
    }

    sourceSets {
        iosMain {
            kotlin.srcDir("../app/src/main/java")
            kotlin.exclude(
                "com/brenninhotools/musicstuff/MainActivity.kt",
                "com/brenninhotools/musicstuff/android/**",
                "com/brenninhotools/musicstuff/jvm/**",
            )
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
    }
}
