# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# WebView JavaScript interface — annotatsiyaga asoslanib saqlanadi (klass yo'li o'zgarsayam ishlaydi)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Firebase
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# KidZone public API
-keep public class uz.kidzone.app.** { public *; }
