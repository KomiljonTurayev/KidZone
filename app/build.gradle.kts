import java.util.Properties
import java.io.FileInputStream

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    kotlin("kapt")
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) localProps.load(FileInputStream(localPropsFile))
val aishaTtsApiKey = System.getenv("AISHA_TTS_API_KEY") ?: localProps.getProperty("AISHA_TTS_API_KEY", "")
val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: localProps.getProperty("GEMINI_API_KEY", "")

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                implementation("dev.gitlive:firebase-auth:1.12.0")
                implementation("dev.gitlive:firebase-firestore:1.12.0")
                implementation("dev.gitlive:firebase-crashlytics:1.12.0")
                
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha06")
                
                // Embedded Offline Web Server for HTML5 games
                implementation("io.ktor:ktor-server-core:2.3.11")
                implementation("io.ktor:ktor-server-cio:2.3.11")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            kotlin.srcDirs("src/androidMain/java")
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.0")
                implementation("androidx.navigation:navigation-compose:2.7.7")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
                implementation("androidx.appcompat:appcompat:1.7.0")
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("com.google.android.material:material:1.12.0")
                implementation("androidx.core:core-splashscreen:1.0.1")
                
                implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.7.0"))
                implementation("com.google.firebase:firebase-auth")
                implementation("com.google.firebase:firebase-firestore")
                implementation("com.google.firebase:firebase-messaging")
                implementation("com.google.firebase:firebase-crashlytics")
                implementation("com.google.firebase:firebase-analytics")
                
                implementation("androidx.work:work-runtime-ktx:2.9.0")
                implementation("androidx.room:room-runtime:2.6.1")
                implementation("androidx.room:room-ktx:2.6.1")
                
                implementation("com.google.firebase:firebase-ai:16.0.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                
                implementation("androidx.webkit:webkit:1.11.0")
            }
        }
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {}
        }
    }
}

android {
    namespace = "uz.kidzone.app"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "uz.kidzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.4.0"
        buildConfigField("String", "AISHA_TTS_API_KEY", "\"${aishaTtsApiKey}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey}\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    dependencies {
        debugImplementation("androidx.compose.ui:ui-tooling")
        debugImplementation("com.google.firebase:firebase-appcheck-debug")
        // Note: Adding pure Android revenuecat here because KMP version requires specific setup.
        implementation("com.revenuecat.purchases:purchases:8.4.0")
        implementation("com.revenuecat.purchases:purchases-ui:8.4.0")
        add("kapt", "androidx.room:room-compiler:2.6.1")
    }
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
