# Release rules. Minification is disabled in this scaffold; enable once you
# want R8 and these rules keep the reflective layers intact.

# kotlinx.serialization reflective access
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.guesswhere.app.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.guesswhere.app.data.**$$serializer { *; }

# Retrofit service interface
-keep interface com.guesswhere.app.data.CommonsApi { *; }

# osmdroid: reflection into map internals
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**