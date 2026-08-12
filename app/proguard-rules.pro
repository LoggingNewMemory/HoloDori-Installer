# ─── HoloDori Installer ProGuard Rules ────────────────────────────────────────

# Disable ALL R8 transformations except dead code removal for unused libraries
-dontoptimize
-dontobfuscate

# ─── Keep our app ─────────────────────────────────────────────────────────────
-keep,allowshrinking class com.kanagawa.yamada.holodoriinstaller.** { *; }

# ─── Shizuku ──────────────────────────────────────────────────────────────────
# Installer.kt line 141 uses reflection: getDeclaredMethod("newProcess", ...)
# Must keep ALL methods including private ones on all Shizuku classes.
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keepclassmembers class moe.shizuku.** { *; }
# Keep the Shizuku binder interfaces used via IPC
-keep class rikka.shizuku.server.** { *; }

# ─── OkHttp + Okio ───────────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── WorkManager + Room ───────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.** { *; }

# ─── AndroidX Startup ────────────────────────────────────────────────────────
-keep class androidx.startup.** { *; }

# ─── AndroidX Lifecycle ──────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ─── SQLite (used by Room internally) ─────────────────────────────────────────
-keep class androidx.sqlite.** { *; }

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# ─── Compose ──────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ─── General ──────────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
