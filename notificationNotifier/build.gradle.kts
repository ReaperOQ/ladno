plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "app.ladno.notifier"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.ladno.notifier"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
}

androidComponents {
    beforeVariants { it.enable = it.buildType == "debug" }
}
