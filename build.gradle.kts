plugins {
    kotlin("kapt") version "2.2.0" apply false
    kotlin("plugin.serialization") version "1.9.23" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.github.triplet.play") version "3.10.1" apply false
    alias(libs.plugins.ksp)
}
