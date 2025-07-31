plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "core_ui"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(compose.foundation)
                api(compose.runtime)
                api(compose.ui)
                api(compose.material)
                api(compose.material3)
                api(libs.decompose)
                api(libs.serialization)
                api(libs.coil3)
                api(libs.coil3Network)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.core.ui"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}