plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "core_inject"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinInject.runtime)
                implementation(libs.logger.napier)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.core.inject"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}