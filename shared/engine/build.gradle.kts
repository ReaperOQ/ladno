@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
}

kotlin {
    android {
        namespace = "app.ladno.engine"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    wasmJs {
        browser()
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.shared.domain)
    }
}
