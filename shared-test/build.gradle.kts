plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cuscrud.shared.test"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java", "src/sharedTest/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java", "src/sharedTest/java")
            assets.srcDirs("src/androidTest/assets")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    // Add other test dependencies that should be shared here
}
