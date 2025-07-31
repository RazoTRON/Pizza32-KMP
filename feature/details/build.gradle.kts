plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    androidTarget()
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "feature.details"
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

//                implementation(projects.ksp)
            }
        }

        jvmMain {
            dependencies {
                api(compose.desktop.macos_arm64)
            }
        }
    }
}

android {
    namespace = "com.app.multicourse.feature.details"

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

    add("kspCommonMainMetadata", projects.ksp)
//    add("kspAndroid", projects.ksp)
//    add("kspIosX64", projects.ksp)
//    add("kspIosArm64", projects.ksp)
//    add("kspIosSimulatorArm64", projects.ksp)
//    add("kspJvm", projects.ksp)
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if(name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}