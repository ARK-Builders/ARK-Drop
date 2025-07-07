plugins {
    kotlin("kapt") version "2.2.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.56.2"
}

android {
    namespace = "dev.arkbuilders.drop.app"
    compileSdk = 36

//    signingConfigs {
//        create("release") {
//            keyAlias = System.getenv("KEY_ALIAS")
//            keyPassword = System.getenv("KEY_PASSWORD")
//            storePassword = System.getenv("KEYSTORE_PASSWORD")
//            storeFile = file(System.getenv("KEYSTORE_PATH"))
//        }
//    }

    defaultConfig {
        applicationId = "dev.arkbuilders.drop.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = System.getenv("RELEASE_VERSION") ?: "dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
//            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
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
    }

    packaging {
        jniLibs.excludes.add("META-INF/AL2.0")
        jniLibs.excludes.add("META-INF/LGPL2.1")
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
    implementation("dev.arkbuilders:drop:1.1.4") {
        artifact {
            extension = "aar"
            type = "aar"
        }
    }

    // QR CODE create setup
    implementation(libs.google.zxing.core)
    implementation(libs.google.zxing.javase)
    implementation(libs.github.yuriy.budiyev.code.scanner)

    // QR CODE SCAN
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.accompanist.permissions)

    // DAGGER SETUP
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-compiler:2.56.2")

    // For instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.56.2")

    // For local unit tests
    testImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    kaptTest("com.google.dagger:hilt-compiler:2.56.2")

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
}

kapt {
    correctErrorTypes = true
}

tasks.named<Delete>("clean") {
    delete(fileTree("$projectDir/src/main/jniLibs"))
}
