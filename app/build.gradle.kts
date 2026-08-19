plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.breath.trainer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.breath.trainer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // 若 -PbreathReleaseSigning=true 且 keystore 文件存在，则用其签名 release；
    // 否则回退到 debug 签名（CI 默认行为：未配置 secrets 时也能产出可安装 APK）。
    val useReleaseSigning = (project.findProperty("breathReleaseSigning") as? String) == "true" &&
            file("keystore/release.jks").exists()

    signingConfigs {
        create("breathRelease") {
            if (useReleaseSigning) {
                storeFile = file("keystore/release.jks")
                storePassword = (project.findProperty("BREATH_RELEASE_STORE_PASSWORD") as? String) ?: ""
                keyAlias = (project.findProperty("BREATH_RELEASE_KEY_ALIAS") as? String) ?: ""
                keyPassword = (project.findProperty("BREATH_RELEASE_KEY_PASSWORD") as? String) ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (useReleaseSigning)
                signingConfigs.getByName("breathRelease")
            else
                signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // AGP 8.6 启用了新的 strip 工具链，androidx.datastore 自带的 .so
        // (libdatastore_shared_counter.so) 用的是旧 ABI 标记，strip 工具无法识别
        // 会报 "Unable to strip the following libraries, packaging them as they are"。
        // 改用 legacy 打包方式，跳过 strip，保持与 Android 24+ 各 ABI 兼容。
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1)

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
