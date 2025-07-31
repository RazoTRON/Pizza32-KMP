plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.compose)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()
    iosArm64()
    iosX64()
    iosSimulatorArm64()


    cocoapods {
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        podfile = project.file("../../iosApp/Podfile")
        ios.deploymentTarget = "17.4"

        framework {
            baseName = "maps"
            isStatic = true
        }

//        pod("GoogleMaps",moduleName = "GoogleMaps", version = "~> 8.4.0", linkOnly = true)
        pod("GoogleMaps") {
            version = "8.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(libs.decompose)
                implementation(libs.decomposeExtensions)

                implementation(libs.kotlinInject.runtime)
                implementation(libs.serialization)

                implementation(projects.core.ui)
                implementation(projects.core.navigation)
                implementation(projects.core.inject)
                implementation(projects.domain)
            }
        }

        jvmMain {
            dependencies {
                api(compose.desktop.macos_arm64)

                implementation("org.jxmapviewer:jxmapviewer2:2.8")
            }
        }

        androidMain {
            dependencies {

                // Google Maps
                implementation(libs.googleMaps)
                implementation(libs.playServicesMaps)
                implementation(libs.play.services.location)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.feature.googleMaps"

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