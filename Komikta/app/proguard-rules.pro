# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.komikta.**$$serializer { *; }
-keepclassmembers class com.komikta.** {
    *** Companion;
}
-keepclasseswithmembers class com.komikta.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Timber
-dontwarn com.jakewharton.timber.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
