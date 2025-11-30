import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.triplet.play)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint.gradle)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmToolchain(11)
    }
}

android {
    namespace = "dev.arkbuilders.drop.app"
    compileSdk = 36

    signingConfigs {
        create("testRelease") {
            storeFile = project.rootProject.file("keystore.jks")
            storePassword = "sw0rdf1sh"
            keyAlias = "ark-builders-test"
            keyPassword = "rybamech"
        }
    }

    defaultConfig {
        applicationId = "dev.arkbuilders.drop.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        setProperty("archivesBaseName", "ark-drop")
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("testRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Enable R8 full mode
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs.excludes.add("META-INF/AL2.0")
        jniLibs.excludes.add("META-INF/LGPL2.1")
        resources.excludes.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // NAVIGATION
    implementation(libs.androidx.navigation.compose)

    // Bindings setup
    implementation(libs.jna) {
        artifact {
            extension = "aar"
            type = "aar"
        }
    }
    //noinspection Aligned16KB
    implementation(libs.arkbuilders.drop) {
        artifact {
            extension = "aar"
            type = "aar"
        }
    }

    // QR CODE create setup
    implementation(libs.google.zxing.core)
    implementation(libs.github.yuriy.budiyev.code.scanner)

    // QR CODE SCAN
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.accompanist.permissions)

    implementation(libs.timber)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.orbit.compose)
    implementation(libs.orbit.viewmodel)

    // EXTRA ICONS
    implementation(libs.simple.icons)
    implementation(libs.font.awesome)
    implementation(libs.tabler.icons)

    // DEVELOPMENT SETUP
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // File-system profile manager
    implementation(libs.io.coil)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.serialization)

    implementation(libs.ark.about)

    // Koin Dependency Injection
    implementation(libs.io.koin.core)
    implementation(libs.io.koin.android)
    implementation(libs.io.koin.compose)
    implementation(libs.io.koin.test)
}

tasks.preBuild.dependsOn(tasks.ktlintCheck)
tasks.ktlintCheck.dependsOn(tasks.ktlintFormat)

tasks.named<Delete>("clean") {
    delete(fileTree("$projectDir/src/main/jniLibs"))
}
