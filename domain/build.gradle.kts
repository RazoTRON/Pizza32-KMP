plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "domain"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.coroutines.core)

                implementation(libs.kotlinInject.runtime)
                implementation(libs.serialization)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.domain"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}