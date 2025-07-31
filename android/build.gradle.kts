import org.jetbrains.compose.internal.utils.localPropertiesFile
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

App.properties.load(localPropertiesFile.inputStream())
App.properties.load(project.rootProject.file("properties/keys.properties").inputStream())
App.properties.load(
    project.rootProject.file("properties/restaurant_config.properties").inputStream()
)


object App {
    const val compileSdk = 34
    const val minSdk = 26
    const val targetSdk = 34
    const val version = "2.1.1"
    const val versionCode = 9
    val properties = Properties()
    val keys = Properties()
}

fun Properties.storeFilePath(): String = getProperty("STORE_FILE")
fun Properties.storePassword(): String = getProperty("STORE_PASSWORD")
fun Properties.keyPassword(): String = getProperty("KEY_PASSWORD")
fun Properties.keyAlias(): String = getProperty("KEY_ALIAS")
fun Properties.keyGoogleMapKey(): String = getProperty("GOOGLE_MAP_API_KEY")

kotlin {
    sourceSets.debug {
        kotlin.srcDir("build/generated/ksp/debug/kotlin")
    }
    sourceSets.release {
        kotlin.srcDir("build/generated/ksp/release/kotlin")
    }
}

android {
    namespace = "com.app.multicourse.android"

    compileSdk = App.compileSdk

    defaultConfig {
        minSdk = App.minSdk
        targetSdk = App.targetSdk
        applicationId = "com.pizza32cm.pizza"
        versionCode = App.versionCode
        versionName = "2.1"

        manifestPlaceholders["GOOGLE_MAP_API_KEY"] = App.properties.keyGoogleMapKey()
    }

    signingConfigs {
        create("release") {
            storeFile = file(App.properties.storeFilePath())
            storePassword = App.properties.storePassword()
            keyPassword = App.properties.keyPassword()
            keyAlias = App.properties.keyAlias()
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["GOOGLE_MAP_API_KEY"] =
                App.properties.getProperty("GOOGLE_MAP_API_RELEASE_KEY")

            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            manifestPlaceholders["GOOGLE_MAP_API_KEY"] =
                App.properties.getProperty("GOOGLE_MAP_API_DEBUG_KEY")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlinInject.runtime)
        ksp(libs.kotlinInject.compiler)
        implementation(projects.shared)

        implementation(kotlin("stdlib"))
        implementation(projects.ksp)
        ksp(projects.ksp)
    }
}