# Keep the app's own entry points and data model names stable. This avoids
# obfuscation surprises around enum values persisted as JSON and the Compose
# entry points while still letting R8 shrink the bundled libraries.
-keep class com.tick.app.** { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn org.jetbrains.annotations.**
