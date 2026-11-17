@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "ladno.js"
            }
        }
        binaries.executable()
    }

    sourceSets.wasmJsMain.dependencies {
        implementation(projects.shared.ui)
        implementation(libs.compose.ui)
    }
}
