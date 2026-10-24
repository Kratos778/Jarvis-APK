plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

import java.util.Base64
import java.io.FileOutputStream

val keystoreFile = file("${rootProject.projectDir}/keystore/jarvis-debug.keystore")
if (!keystoreFile.exists()) {
    val b64File = file("${rootProject.projectDir}/keystore/jarvis-debug.b64")
    if (b64File.exists()) {
        val b64 = b64File.readText().trim()
        keystoreFile.parentFile.mkdirs()
        FileOutputStream(keystoreFile).use { it.write(Base64.getDecoder().decode(b64)) }
    }
}

android {
    namespace = "com.elizierdias.jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elizierdias.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        setProperty("archivesBaseName", "Jarvis")
    }

    signingConfigs {
        create("jarvis") {
            storeFile = keystoreFile
            storePassword = "jarvisdebug"
            keyAlias = "jarvis"
            keyPassword = "jarvisdebug"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("jarvis")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jarvis")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }

    applicationVariants.configureEach {
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Jarvis.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
