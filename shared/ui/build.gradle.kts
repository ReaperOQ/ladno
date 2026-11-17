@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "app.ladno.ui"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    wasmJs {
        browser()
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.shared.data)
        implementation(projects.shared.designsystem)
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui)
    }
}
