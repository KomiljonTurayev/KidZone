# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# WebView JavaScript interface
-keepclassmembers class uz.kidzone.app.MainActivity$AdMobBridge {
    public *;
}
-keepattributes JavascriptInterface

# Keep MainActivity
-keep class uz.kidzone.app.** { *; }
