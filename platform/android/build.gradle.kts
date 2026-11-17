plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.ladno.platform.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

dependencies {
    implementation(projects.shared.data)
}
