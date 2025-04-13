import com.google.android.libraries.mapsplatform.secrets_gradle_plugin.loadPropertiesFile
import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maps.secrets)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.protobuf)
}

val secrets = rootProject.loadPropertiesFile("secrets.properties")

android {
    namespace = "me.martelli.wheresmycar"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "me.martelli.wheresmycar"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = "upload"
            keyPassword = secrets.getProperty("keyPassword", "")
            storeFile = file(secrets.getProperty("storeFile", "."))
            storePassword = secrets.getProperty("storePassword", "")
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

    androidResources {
        @Suppress("UnstableApiUsage")
        localeFilters += setOf("en", "it")
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjspecify-annotations=strict", "-Xtype-enhancement-improvements-strict-mode")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            excludes += "**/libdatastore_shared_counter.so"
        }
    }

    sourceSets {
        named("main") {
            java {
                srcDir("build/generated/sources/proto/main/java")
            }
            proto {
                srcDir("src/main/proto")
            }
        }
    }
}

secrets {
    propertiesFileName = "secrets.properties"
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
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
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore)
    implementation(libs.protobuf.javalite)
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