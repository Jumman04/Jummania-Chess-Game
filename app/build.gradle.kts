import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jummania.chess_game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jummania.chess_game"
        minSdk = 23
        targetSdk = compileSdk
        versionCode = 1
        versionName = "1.0"

        resValue("string", "versionName", versionName.toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":chess_game"))
    implementation(libs.android.colorpickerpreference)
    implementation(libs.androidx.preference)
    implementation(libs.okhttp)
}