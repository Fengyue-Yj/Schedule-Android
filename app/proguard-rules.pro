# ProGuard rules for Schedule Android app

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Jsoup
-keep public class org.jsoup.** { public *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.schedule.app.teaching.** { *; }
-keep class com.schedule.app.data.models.** { *; }
