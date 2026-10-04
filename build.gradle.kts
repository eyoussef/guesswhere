// AGP 9 built-in Kotlin defaults to KGP 2.2.10; the 2026 AndroidX artifacts ship
// newer Kotlin metadata. Raise the runtime KGP the documented way:
// https://developer.android.com/build/migrate-to-built-in-kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

// Top-level build file: apply nothing here, only declare plugin availability.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}