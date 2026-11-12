import java.util.Properties
import java.io.FileInputStream

private val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
private val revenueCatApiKey = System.getenv("REVENUECAT_API_KEY") ?: localProps.getProperty("REVENUECAT_API_KEY", "")

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

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
                
                implementation(libs.gitlive.firebase.auth)
                implementation(libs.gitlive.firebase.firestore)
                implementation(libs.gitlive.firebase.crashlytics)
                
                implementation(libs.kotlinx.coroutines.core)
                // Avatars are local files only, so no Coil network module is needed.
                implementation(libs.coil.compose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockito.core)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
                // AGP's stub android.jar makes every org.json.JSONObject/JSONArray constructor
                // throw "not mocked" (isReturnDefaultValues doesn't cover constructors). This
                // real, non-Android org.json implementation shares the same package/class names
                // and — per Gradle's test runtime classpath ordering — takes precedence over
                // the stub, so ContentFilter/DailyChallengeRepository's real JSON parsing runs.
                implementation(libs.json)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.core.ktx)
                implementation(libs.material)
                implementation(libs.androidx.core.splashscreen)
                
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.auth)
                implementation(libs.firebase.firestore)
                implementation(libs.firebase.messaging)
                implementation(libs.firebase.crashlytics)
                implementation(libs.firebase.analytics)

                // "Sign in with Google" via Credential Manager (see auth/GoogleSignIn.kt)
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)

                implementation(libs.androidx.work.runtime.ktx)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.room.ktx)
                
                implementation(libs.firebase.ai)
                implementation(libs.okhttp)
                
                implementation(libs.androidx.webkit)
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
    compileSdk = 36

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "uz.kidzone.app"
        minSdk = 26
        targetSdk = 36
        val propVersionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull()
        val propVersionName = project.findProperty("versionName")?.toString()
        versionCode = propVersionCode ?: 10602
        versionName = propVersionName ?: "1.6.2"
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${revenueCatApiKey}\"")
    }

    val keystoreProps = Properties()
    val keystoreFile = file("keystore.properties")
    val rootKeystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        keystoreProps.load(FileInputStream(keystoreFile))
    } else if (rootKeystoreFile.exists()) {
        keystoreProps.load(FileInputStream(rootKeystoreFile))
    }

    signingConfigs {
        // Committed project debug key (standard "android" passwords, not a secret). Every
        // machine and every CI run signs debug builds with the same SHA-1, which Firebase
        // must know for Google Sign-In; the per-machine default key changes on each CI runner.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val envKeystore = System.getenv("KEYSTORE_FILE")
            val resolvedEnv = envKeystore?.let {
                file(it).takeIf { f -> f.exists() }
                    ?: rootProject.file(it).takeIf { f -> f.exists() }
                    ?: file("app/$it").takeIf { f -> f.exists() }
            }
            if (resolvedEnv != null) {
                storeFile = resolvedEnv
                storePassword = System.getenv("STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            } else if (keystoreProps.containsKey("storeFile")) {
                val sFile = keystoreProps.getProperty("storeFile")
                val resolved = file(sFile).takeIf { it.exists() }
                    ?: rootProject.file(sFile).takeIf { it.exists() }
                    ?: file("app/$sFile").takeIf { it.exists() }
                if (resolved != null) {
                    storeFile = resolved
                    storePassword = keystoreProps.getProperty("storePassword", "")
                    keyAlias = keystoreProps.getProperty("keyAlias", "")
                    keyPassword = keystoreProps.getProperty("keyPassword", "")
                }
            }
        }
    }

    buildTypes {
        getByName("release") {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            // AGP's default unit-test android.jar throws "not mocked" from every framework
            // method (e.g. android.util.Log.d/w) instead of running real logic. Most of this
            // module's tests only rely on Log not crashing, not on what it logs.
            isReturnDefaultValues = true
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    dependencies {
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.firebase.appcheck.debug)
        // Note: Adding pure Android revenuecat here because KMP version requires specific setup.
        implementation(libs.revenuecat.purchases)
        implementation(libs.revenuecat.purchases.ui)

        // src/test/java's FirebaseManagerTest/PinUtilTest/ParentalStatsManagerTest/
        // KidZoneFirebaseMessagingServiceTest have referenced these since before this
        // module's history starts, but nothing ever declared them — silently broken because
        // CI never runs testDebugUnitTest, only assembleDebug/bundleRelease.
        testImplementation(libs.junit)
        testImplementation(libs.mockito.core)
        testImplementation(libs.robolectric)
    }
}

// Room's annotation processor, via KSP for the Android target (replaces kapt).
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}
