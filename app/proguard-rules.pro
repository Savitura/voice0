# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.voice0.app.**$$serializer { *; }
-keepclassmembers class com.voice0.app.** { *** Companion; }
-keepclasseswithmembers class com.voice0.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Solana
-keep class com.solana.** { *; }
-keep class com.solanamobile.** { *; }
-keep class io.github.funkatronics.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
