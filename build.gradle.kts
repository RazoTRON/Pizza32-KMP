plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.cocoapods) apply false
    alias(libs.plugins.compose) apply false

    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.jvm) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

//    task("compileJava").doLast {
//        println("Dummy compileJava task:)")
//    }
//    task("testClasses").doLast {
//        println("Dummy testClasses task:)")
//    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            freeCompilerArgs += "-Xexpect-actual-classes"
        }
    }
}