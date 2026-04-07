plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.psnp.app"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.psnp.app"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = (findProperty("app.versionCode") as? String)?.toInt() ?: 1
        versionName = (findProperty("app.versionName") as? String) ?: "1.0.0"
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file(project.findProperty("signing.storeFile") ?: "debug.keystore")
            storePassword = project.findProperty("signing.storePassword") as? String ?: "android"
            keyAlias = project.findProperty("signing.keyAlias") as? String ?: "androiddebugkey"
            keyPassword = project.findProperty("signing.keyPassword") as? String ?: "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val versionName = variant.versionName
            output.outputFileName = "PSNP-v${versionName}-debug.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
