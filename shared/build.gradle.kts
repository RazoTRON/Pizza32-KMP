import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
        podfile = project.file("../iosApp/Podfile")
        ios.deploymentTarget = "17.4"

        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material)
                api(projects.domain)
                api(projects.data)
                api(projects.feature.home)
                api(projects.feature.details)
                api(projects.feature.bottomBar)
                api(projects.feature.topBar)
                api(projects.feature.about)
                api(projects.feature.favourites)
                api(projects.feature.search)
                api(projects.feature.contactInfo)
                api(projects.feature.cart)
                api(projects.core.ui)
                api(projects.core.navigation)
                api(projects.core.inject)

                implementation(libs.kotlinInject.runtime)
                api(libs.decompose)
                implementation(libs.decomposeExtensions)
                implementation(libs.serialization)

                api(libs.ktor.client.core)
                api(libs.ktor.serialization.kotlinx.json)
            }
        }

        iosMain {
            kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")

            dependencies {
                implementation(libs.kotlinInject.runtime)

                api(libs.ktor.client.darwin)
            }
        }

        jvmMain {
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")

            dependencies {
                api(compose.desktop.macos_arm64)

                implementation(libs.kotlinInject.runtime)
            }
        }

        androidMain {
            kotlin.srcDir("build/generated/ksp/android/androidDebug/kotlin")

            dependencies {
                implementation(libs.kotlinInject.runtime)
                implementation((libs.decompose))

                api(libs.ktor.client.android)

                // Google maps for Compose
                api("com.google.android.gms:play-services-location:21.0.1")
                api("com.google.android.gms:play-services-maps:18.1.0")
                implementation("com.google.maps.android:maps-compose:3.1.0")
                // For clustering
                implementation("com.google.maps.android:maps-compose-utils:3.1.0")
            }
        }
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

android {
    namespace = "com.app.multicourse.shared"

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .flatMap { it.binaries }
        .forEach { compilationUnit -> compilationUnit.linkerOpts("-lsqlite3") }
}
