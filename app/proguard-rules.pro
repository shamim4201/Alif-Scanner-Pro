# Project ProGuard Rules for Alif Scanner Pro

# Keep line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ZXing QR and Barcode Engine
-keep class com.google.zxing.** { *; }

# Google AdMob
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.ads.** { public *; }

# ML Kit Vision & Recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
