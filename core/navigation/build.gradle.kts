plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "core_navigation"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.decompose)
                api(libs.serialization)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.core.navigation"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}