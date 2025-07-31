enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()

        maven("https://jogamp.org/deployment/maven") // KMP WebView
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Pizza32"
include(":domain")
include(":shared")
include(":desktop")
include(":android")
include(":data")
include(":feature:home")
include(":feature:details")
include(":feature:bottom_bar")
include(":feature:top_bar")
include(":feature:about")
include(":feature:maps")
include(":feature:favourites")
include(":iosApp")
include(":core:ui")
include(":core:navigation")
include(":core:inject")
include(":feature:search")
include(":feature:cart")
include(":feature:contactInfo")
//include(":ksp")
include(":ksp")
