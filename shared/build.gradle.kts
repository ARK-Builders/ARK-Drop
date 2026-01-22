import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.type
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint.gradle)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.orbit.core)
            implementation(libs.orbit.viewmodel)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kermit)
            implementation(libs.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.google.zxing.core)
            implementation(libs.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.jna.get().toString()) {
                artifact {
                    type = "aar"
                    extension = "aar"
                }
            }
            implementation(libs.arkbuilders.drop)
            implementation(libs.timber)
        }
    }
}

android {
    namespace = "dev.arkbuilders.drop.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    // add("kspIosX64", libs.androidx.room.compiler)
    // Add any other platform target you use in your project, for example kspDesktop
}

room {
    schemaDirectory("$projectDir/schemas")
}
