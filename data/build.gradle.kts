plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "data"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.domain)
                implementation(projects.core.inject)

                implementation(libs.kotlinInject.runtime)
                implementation(libs.serialization)

                implementation(projects.core.navigation)

                //Sqldelight
                implementation(libs.sqldelight.coroutines.extensions)

                implementation(libs.dateTimeKotlinX)

                //Network
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                implementation(libs.logger.napier)

                api(libs.preferences)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.ktor.client.android)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.desktop.driver)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqldelight.native.driver)
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

sqldelight {
    databases {
        create("AppDb") {
            packageName.set("com.app.multicourse.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}

android {
    namespace = "com.app.multicourse.data"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // KSP will eventually have better multiplatform support and we'll be able to simply have
    // `ksp libs.kotlinInject.compiler` in the dependencies block of each source set
    // https://github.com/google/ksp/pull/1021
    add("kspIosX64", libs.kotlinInject.compiler)
    add("kspIosArm64", libs.kotlinInject.compiler)
    add("kspIosSimulatorArm64", libs.kotlinInject.compiler)
    add("kspJvm", libs.kotlinInject.compiler)
    add("kspAndroid", libs.kotlinInject.compiler)
}