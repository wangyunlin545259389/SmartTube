# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/google/home/cartland/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Begin Enable minification

#-ignorewarnings
#-dontobfuscate

-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
#-keepattributes InnerClasses

#-keep public class com.liskovsoft.youtubeapi.** { *; }
#-keep class okhttp3.**{*;}
#-keep interface okhttp3.** { *; }

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn com.jayway.jsonpath.**
-dontwarn retrofit2.**
-dontwarn org.slf4j.**

# 保留 KTVActivity 类名及所有 public 方法（包括生命周期方法）
#-keep public class com.liskovsoft.smartyoutubetv2.tv.ui.search.KTVActivity {
#    public *;
#}

# 保留所有类，但排除 KTVActivity 和它的内部类/匿名类
# 这样：你的其他源码、project(...) 模块、外部依赖、libs/*.jar 都不会被混淆
-keep class !com.liskovsoft.smartyoutubetv2.tv.ui.search.KTVActivity,!com.liskovsoft.smartyoutubetv2.tv.ui.search.KTVActivity$*,** { *; }

# KTVActivity 是 Activity，类名保留；只让 private 字段/方法/内部实现混淆
-keep,allowoptimization class com.liskovsoft.smartyoutubetv2.tv.ui.search.KTVActivity {
    public protected <fields>;
    public protected <methods>;
}

# 保留反射、泛型、内部类相关元信息
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# 不让 R8 重新打包类名
#-dontrepackage
# End Enable minification
