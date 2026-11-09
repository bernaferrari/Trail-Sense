plugins {
    kotlin("multiplatform") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    kotlin("plugin.serialization") version "2.4.21" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.1" apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    id("com.vanniktech.maven.publish") version "0.37.0"
}
