import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.loadPropertiesFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maps.secrets)
    alias(libs.plugins.baselineprofile)
}

val secrets = rootProject.loadPropertiesFile("secrets.properties")

android {
    namespace = "me.martelli.wheresmycar"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "me.martelli.wheresmycar"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resourceConfigurations += listOf("en", "it")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = "upload"
            keyPassword = secrets["keyPassword"] as String
            storeFile = file(secrets["storeFile"] as String)
            storePassword = secrets["storePassword"] as String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs["release"]

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    baselineProfile {
        dexLayoutOptimization = true
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            excludes += "**/libdatastore_shared_counter.so"
        }
    }
}

secrets {
    propertiesFileName = "secrets.properties"
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.maps)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.work.runtime)
    implementation(libs.splashscreen)
    implementation(libs.core.google.shortcuts)
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}