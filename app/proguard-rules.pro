# Default proguard rules. Keep Kotlinx Serialization metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.gabriion.betterme.**$$serializer { *; }
-keepclassmembers class com.gabriion.betterme.** {
    *** Companion;
}
-keepclasseswithmembers class com.gabriion.betterme.** {
    kotlinx.serialization.KSerializer serializer(...);
}
