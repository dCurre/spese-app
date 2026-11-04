# Regole ProGuard/R8 per la build release.

# ---------------------------------------------------------------------------
# kotlinx.serialization
# Senza queste regole, R8 rimuove/rinomina i serializer generati e i tipi dei
# modelli, e Retrofit fallisce con "Unable to create converter for class ...".
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Mantieni il companion dei @Serializable e i metodi serializer()
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Mantieni le classi dei modelli annotate @Serializable (nomi/campi) e i loro serializer
-keep,includedescriptorclasses class com.dcapps.spese.**$$serializer { *; }
-keepclassmembers class com.dcapps.spese.** {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class com.dcapps.spese.** { *; }

# Classi di supporto del runtime kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Retrofit / OkHttp
# ---------------------------------------------------------------------------
# Mantieni le firme generiche (fondamentale per i converter basati sui tipi)
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Interfacce Retrofit (metodi suspend compresi)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Le interfacce con annotazioni Retrofit non vanno offuscate nelle firme
-keep interface com.dcapps.spese.core.network.** { *; }
-keep interface com.dcapps.spese.core.update.** { *; }

# OkHttp: avvisi innocui
-dontwarn okhttp3.**
-dontwarn okio.**
