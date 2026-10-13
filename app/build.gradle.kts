import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // To rename the app's package later, change both namespace and applicationId.
    namespace = "com.universalazan.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.universalazan.app"
        // Android 8.0. Gives us built-in java.time (time zones / daylight saving)
        // and notification channels without extra libraries. Do not lower casually.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing. The key and its passwords are never in this project or in Git: the
    // machine-wide Gradle settings (GRADLE_USER_HOME/gradle.properties) name a properties file
    // kept elsewhere, e.g. universalAzan.signingProperties=F:/UniversalAzan-keys/signing.properties
    // Without it the release APK is built unsigned. See docs/release.md.
    val signingFile = providers.gradleProperty("universalAzan.signingProperties").orNull?.let { file(it) }
    val releaseSigning = if (signingFile != null && signingFile.isFile) {
        val props = Properties().apply { signingFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else {
        null
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**", "kotlin.**", "kotlinx.**",
                    "com.batoulapps.**", "com.universalazan.app.**",
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    // Azan audio: use personal recordings if present (src/personalAudio, not in Git),
    // otherwise the clearly labelled test tones in src/testAudio. Both define the same
    // resource names (raw/azan_full, raw/azan_full_fajr, raw/azan_short).
    sourceSets {
        getByName("main") {
            val personalAudio = file("src/personalAudio/res")
            val audioRes = if (personalAudio.resolve("raw").isDirectory) personalAudio else file("src/testAudio/res")
            res.directories.add(audioRes.path)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.adhan)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
