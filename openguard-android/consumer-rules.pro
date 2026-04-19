# OpenGuard Android Consumer ProGuard Rules
# These rules are applied to apps that depend on openguard-android.

# Keep all OpenGuard public API classes
-keep class com.openguard.** { *; }
-keepclassmembers class com.openguard.** { *; }

# Keep enum values
-keepclassmembers enum com.openguard.** { *; }

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-keepattributes EnclosingMethod

# OkHttp (if using OpenGuard network module)
-dontwarn okhttp3.**
-dontwarn okio.**
