@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
}

kotlin {

    jvm()

    sourceSets {

        commonMain {
            dependencies {
                implementation(projects.core.navigation)
                implementation(libs.decompose)
                implementation(libs.kotlinInject.runtime)
                implementation("com.squareup:kotlinpoet:1.18.1")
                implementation("com.google.devtools.ksp:symbol-processing-api:1.9.23-1.0.20")
            }
//            kotlin.srcDir("src/main/kotlin")
//            resources.srcDir("src/main/resources")
        }

    }
}
