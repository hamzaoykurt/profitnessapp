# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
-keep,includedescriptorclasses class com.avonix.profitness.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.avonix.profitness.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Room — entity, DAO, database classes
-keep class com.avonix.profitness.data.local.** { *; }

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# App model classes (Supabase JSON serialization)
-keep class com.avonix.profitness.data.** { *; }
-keep class com.avonix.profitness.domain.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
