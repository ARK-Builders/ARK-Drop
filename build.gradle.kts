plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp)
  //  alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.triplet.play) apply false
    alias(libs.plugins.ktlint.gradle) apply false
}
