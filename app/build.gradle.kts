// Configuración del módulo de la app: SDK, tipos de compilación y dependencias.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.peter1323.litrolow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.peter1323.litrolow"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.1"
    }

    buildTypes {
        release {
            // Una lista de miles de filas solo va fluida con el código optimizado por R8.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // shortcut: firma con la clave de depuración para instalar sin configurar nada; cambiarla antes de publicar en Google Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    splits {
        abi {
            // Una APK por arquitectura de móvil y otra universal que sirve en cualquier dispositivo.
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.osmdroid)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
