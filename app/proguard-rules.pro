# Keep Kotlinx Serialization serializers and Serializable models/navigation routes
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.jubayerhossain.sobjidokan.data.local.**$$serializer { *; }
-keep,includedescriptorclasses class com.jubayerhossain.sobjidokan.data.backup.**$$serializer { *; }
-keep,includedescriptorclasses class com.jubayerhossain.sobjidokan.ui.navigation.**$$serializer { *; }

-keepclassmembers class com.jubayerhossain.sobjidokan.** {
    @kotlinx.serialization.Serializable <methods>;
}

# Keep Room database, entities, DAOs and generated implementations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Preserve source file and line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
