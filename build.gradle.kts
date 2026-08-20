// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Kotlin 2.0 起 Compose 编译器插件独立成 Gradle plugin（必须与 kotlin 版本一致）
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
