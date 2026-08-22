# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep TTS classes used at runtime
-keep class com.breath.trainer.audio.** { *; }

# 保留 Compose 运行时与 UI 平台类，防止 R8 的 optimize 阶段把
# LocalLifecycleOwner 等 CompositionLocal 的 provider 误删。
# 否则 release 启动会报：
#   java.lang.IllegalStateException: CompositionLocal LocalLifecycleOwner not present
# （collectAsStateWithLifecycle 内部读取 LocalLifecycleOwner.current）
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.lifecycle.runtime.compose.** { *; }

# 保留注解/签名等属性，兼容 Compose 编译器元数据与潜在反射/序列化
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
