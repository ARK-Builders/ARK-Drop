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
    alias(libs.plugins.skie)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }

        // Configure cinterop for ArkDrop bridge
        iosTarget.compilations.getByName("main") {
            val arkDropBridgeCinterop = cinterops.create("ArkDropBridge") {
                defFile(project.file("src/nativeInterop/cinterop/ArkDropBridge.def"))
                packageName("dev.arkbuilders.drop.bridge")

                // Add include paths - use File objects for proper resolution
                val iosAppPath = rootProject.projectDir.resolve("iosApp/iosApp")
                compilerOpts(
                    "-framework", "Foundation",
                    "-I${iosAppPath.absolutePath}"
                )

                // Specify where to find headers
                includeDirs(iosAppPath.absolutePath)
            }

            // Ensure cinterop runs before Kotlin compilation
            compileTaskProvider.configure {
                val cinteropTaskName = "cinteropArkDropBridge${iosTarget.name.replaceFirstChar { it.uppercase() }}"
                dependsOn(cinteropTaskName)
            }
        }

        // Link SystemConfiguration framework for network monitoring
        iosTarget.binaries.all {
            linkerOpts += listOf("-framework", "SystemConfiguration")
        }
    }

    sourceSets {
        // Common
        val commonMain by getting {
            dependencies {
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
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        // Android
        val androidMain by getting {
            dependencies {
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

        // iOS platform source sets
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        // Create shared iosMain
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting

        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
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
    add("kspIosX64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
