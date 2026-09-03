# WebView JavaScript interface — annotatsiyaga asoslanib saqlanadi (klass yo'li o'zgarsayam ishlaydi)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Firebase (Android & KMP GitLive)
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# KidZone Models (Only keep data classes if reflection is used, otherwise obfuscate them too)
-keep class uz.kidzone.app.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Ktor & Coroutines
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
