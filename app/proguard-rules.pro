# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Aggressive obfuscation
-repackageclasses ''
-allowaccessmodification

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SQLCipher - keep only necessary classes
-keep class net.sqlcipher.database.SQLiteDatabase { *; }
-keep class net.sqlcipher.database.SupportFactory { *; }
-dontwarn net.sqlcipher.**

# Hilt
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends android.app.Application

# EncryptedSharedPreferences - keep only the annotation
-keepclassmembers class * {
    @androidx.security.crypto.EncryptedSharedPreferences *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Firebase - use consumer proguard files from libraries instead
# Only keep specific Firebase classes you use directly
-keep class com.google.firebase.analytics.FirebaseAnalytics { *; }
-keep class com.google.firebase.crashlytics.FirebaseCrashlytics { *; }
-keep class com.google.firebase.remoteconfig.FirebaseRemoteConfig { *; }
-dontwarn com.google.firebase.**

# CameraX - use consumer proguard files
-dontwarn androidx.camera.**

# Billing - keep only interface classes
-keep interface com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**

# Coroutines
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <name>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
