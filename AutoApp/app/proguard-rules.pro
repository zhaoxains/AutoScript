# Add project specific ProGuard rules here.

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations

-keep class com.auto.app.data.model.** { *; }
-keep class com.auto.app.data.remote.request.** { *; }
-keep class com.auto.app.data.remote.response.** { *; }

-keep class * extends android.accessibilityservice.AccessibilityService { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class * {
    public <init>(android.content.Context);
}

-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**

-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.** { *; }

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
