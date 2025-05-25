@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.proto
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maps.secrets)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.protobuf)
}

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
        versionCode = getVersionCode()
        versionName = "2025.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val file = rootProject.file("release.properties")
            if (file.exists()) {
                val secrets = Properties().apply {
                    load(file.inputStream())
                }

                keyAlias = "upload"
                keyPassword = secrets["keyPassword"] as String
                storeFile = file(secrets["storeFile"] as String)
                storePassword = secrets["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        generateLocaleConfig = true
        localeFilters += setOf("en", "it")
    }

    buildFeatures {
        buildConfig = true
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
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList += listOf("keyPassword", "storeFile", "storePassword")
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
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
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
    implementation(libs.haze)
    implementation(libs.haze.materials)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

fun getVersionCode(): Int {
    return file(".version").run { if (exists()) readText().toInt() else 1 }
}

abstract class IncrementVersionCode : DefaultTask() {
    @get:OutputFile abstract val versionFile: RegularFileProperty

    @TaskAction
    fun action() {
        val file = versionFile.get().asFile
        val code = if (file.exists()) file.readText().toInt() else 0

        file.writeText((code + 1).toString())
    }
}

tasks.register<IncrementVersionCode>("incrementVersionCode") {
    versionFile = layout.projectDirectory.file(".version")
}

tasks.register("fullVersion") {
    val name = android.defaultConfig.versionName!!
    val code = android.defaultConfig.versionCode!!

    doLast {
        println("$name+$code")
    }
}
