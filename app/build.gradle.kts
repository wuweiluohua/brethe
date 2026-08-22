import java.util.Properties

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

    // 从 local.properties（已被 .gitignore，放密钥最安全）与 -P 命令行参数中读取发布签名信息。
    // CI 通过 GitHub Secrets 以 -P 形式注入；本地开发把同一组值写进 local.properties 即可。
    val localProps = Properties().also { props ->
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { props.load(it) }
    }
    fun releaseProp(name: String, default: String = ""): String {
        val fromCli = project.findProperty(name)
        if (fromCli is String) return fromCli
        return localProps.getProperty(name) ?: default
    }

    signingConfigs {
        // 固定 debug 密钥：已提交进仓库（keystore/debug.keystore），本地与 CI 共用同一把 →
        // 覆盖安装不再因"每次构建重新生成 debug 钥匙"而报证书冲突。debug 密钥不涉密，可入库。
        create("breathDebug") {
            // 仓库根目录 keystore/（与 local.properties 同级的 rootProject.file 语义一致）
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // 固定 release 密钥：keystore/release.jks（已被 .gitignore，不入库）。
        // 密码从 local.properties 或 -P 传入。
        create("breathRelease") {
            // 仓库根目录 keystore/（release.jks 由 CI 从 Secret 还原到此处）
            storeFile = rootProject.file("keystore/release.jks")
            storePassword = releaseProp("BREATH_RELEASE_STORE_PASSWORD")
            keyAlias = releaseProp("BREATH_RELEASE_KEY_ALIAS", "breath")
            keyPassword = releaseProp("BREATH_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // 用固定 debug 钥匙签名，覆盖安装不再证书冲突
            signingConfig = signingConfigs.getByName("breathDebug")
        }
        release {
            // 重新开启 R8（混淆 + 压缩）。已定位并修复发布版启动闪退：
            // R8 的 optimize 曾把 Compose 的 LocalLifecycleOwner CompositionLocal provider 误删，
            // 导致 collectAsStateWithLifecycle 报 "CompositionLocal LocalLifecycleOwner not present"。
            // proguard-rules.pro 已加 -keep 规则保留 Compose/Lifecycle 相关类，避免 provider 被剥离。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 正式版固定用 release.jks（不再回退到 debug 钥匙）
            signingConfig = signingConfigs.getByName("breathRelease")
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
    implementation("androidx.activity:activity-compose:1.9.1")

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
