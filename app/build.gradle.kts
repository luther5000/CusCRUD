/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Função para ler propriedades do arquivo local.properties
fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    return properties.getProperty(key) ?: ""
}

android {
    namespace = "com.cuscrud"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cuscrud"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.cuscrud.CustomTestRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += "room.incremental" to "true"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            testProguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguardTest-rules.pro")
            
            val baseUrl = getLocalProperty("API_BASE_URL").ifEmpty { "http://localhost/api/v1/" }
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            testProguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguardTest-rules.pro")
            buildConfigField("String", "BASE_URL", "\"https://api.cuscrud.com/v1/\"")
        }
    }

    sourceSets {
        val sharedTestDir = "src/sharedTest/java"
        getByName("test") {
            java.srcDir(sharedTestDir) // Adiciona sem substituir o padrão
        }
        getByName("androidTest") {
            java.srcDir(sharedTestDir) // Adiciona sem substituir o padrão
            assets.srcDir("src/androidTest/assets")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    // App dependencies
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    implementation(libs.androidx.test.espresso.idling.resources)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.dataStore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.hilt.android.core)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofitKotlinxSerializationJson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Jetpack Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(libs.androidx.activity.compose)
    implementation(composeBom)
    implementation(libs.androidx.compose.foundation.core)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.appcompat.theme)
    implementation(libs.accompanist.swiperefresh)

    // Test dependencies
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation("io.mockk:mockk:1.13.12")
    
    // Android Test dependencies
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(project(":shared-test"))
}
