plugins {
    kotlin("kapt") version "2.2.0"
    kotlin("plugin.serialization") version "1.9.23"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.57.1"
    id("com.github.triplet.play") version "3.10.1"
    alias(libs.plugins.ksp)
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
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        setProperty("archivesBaseName", "ark-drop")
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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
                "proguard-rules.pro"
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs.excludes.add("META-INF/AL2.0")
        jniLibs.excludes.add("META-INF/LGPL2.1")
        resources.excludes.addAll(listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/license.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/notice.txt",
            "META-INF/ASL2.0",
            "META-INF/*.kotlin_module"
        ))
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
    implementation("dev.arkbuilders:drop:17348879247") {
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

    // DAGGER SETUP
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")

    // EXTRA ICONS
    implementation("br.com.devsrsouza.compose.icons:simple-icons:1.1.0")
    implementation("br.com.devsrsouza.compose.icons:font-awesome:1.1.0")
    implementation("br.com.devsrsouza.compose.icons:tabler-icons:1.1.0")

    // DEVELOPMENT SETUP
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // File-system profile manager
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.foundation:foundation:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

kapt {
    correctErrorTypes = true
}

tasks.named<Delete>("clean") {
    delete(fileTree("$projectDir/src/main/jniLibs"))
}

