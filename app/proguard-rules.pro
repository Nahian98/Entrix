# ============================================================================
# Entrix — R8 / ProGuard rules
# Most libraries (Room, Compose, Play Services, Koin, Coroutines) ship consumer
# rules, so only project-specific concerns are declared here.
# ============================================================================

# ---- kotlinx.serialization ----
# Type-safe Navigation routes (:core:navigation) are @Serializable objects. R8 must keep their
# generated serializers and the object INSTANCE / Companion, or navigation breaks at runtime.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the app's @Serializable route types (allow obfuscation/optimization of the rest).
-keep,allowobfuscation,allowoptimization @kotlinx.serialization.Serializable class com.apexbytestudio.entrix.**
