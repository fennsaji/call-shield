# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Ktor / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Supabase SDK
-keep class io.github.jan.supabase.** { *; }

# Hilt — keep generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room — keep entity + DAO classes
-keep class com.fenn.callguard.data.local.entity.** { *; }
-keep class com.fenn.callguard.data.local.dao.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlinx serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
