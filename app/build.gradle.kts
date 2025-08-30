import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.util.Properties

val localProps = Properties().apply {
    file("$rootDir/local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun localProp(key: String): String? = localProps.getProperty(key)
val devKeystorePath = "${layout.buildDirectory.asFile.get().absolutePath}/drop-keystore.jks"
val devKeystorePassword = localProp("dev.keystore.password") ?: "defaultPassword123"
val devKeyAlias = "drop-key"
val devDname = localProp("dev.keystore.dname") ?: "CN=Unknown, OU=Dev, O=Unknown, L=City, ST=State, C=XX"

plugins {
    kotlin("kapt") version "2.2.0"
    kotlin("plugin.serialization") version "1.9.23"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.56.2"
    id("com.github.triplet.play") version "3.10.1"
}

android {
    namespace = "dev.arkbuilders.drop.app"
    compileSdk = 36

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: devKeyAlias
            keyPassword = System.getenv("KEY_PASSWORD") ?: devKeystorePassword
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: devKeystorePassword
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: devKeystorePath)
        }
    }

    defaultConfig {
        applicationId = "dev.arkbuilders.drop.app"
        minSdk = 29
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Play Store metadata
        setProperty("archivesBaseName", "drop-v$versionName")
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
            signingConfig = signingConfigs.getByName("release")
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

play {
    serviceAccountCredentials.set(file("play-store-credentials.json"))
    track.set("internal") // Start with internal testing
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    defaultToAppBundles.set(true)
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

    // File-system profile manager
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.compose.foundation:foundation:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

kapt {
    correctErrorTypes = true
}

fun getVersionCode(): Int {
    val versionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
    return versionCode ?: (System.currentTimeMillis() / 1000).toInt()
}

fun getVersionName(): String {
    val versionName = System.getenv("VERSION_NAME")
    return versionName ?: "1.0.0"
}

tasks.named<Delete>("clean") {
    delete(fileTree("$projectDir/src/main/jniLibs"))
}

tasks.register<Exec>("generateDevKeystore") {
    doFirst {
        mkdir(layout.buildDirectory)
    }
    val keystoreFile = file(devKeystorePath)
    commandLine = if (keystoreFile.exists()) {
        listOf("echo", "\"Development keystore already exists.\"")
    } else {
        listOf(
            "keytool", "-genkeypair",
            "-alias", devKeyAlias,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-keystore", devKeystorePath,
            "-storepass", devKeystorePassword,
            "-keypass", devKeystorePassword,
            "-dname", devDname
        )
    }
}

tasks.named("preBuild").dependsOn("generateDevKeystore")

// Task to generate release notes
tasks.register("generateReleaseNotes") {
    doLast {
        val releaseNotesFile = file("fastlane/metadata/android/en-US/changelogs/${getVersionCode()}.txt")
        releaseNotesFile.parentFile.mkdirs()
        releaseNotesFile.writeText("""
            • Initial release of Drop
            • Secure file sharing between devices
            • Profile management with custom avatars
            • Transfer history tracking
            • QR code sharing for easy connections
        """.trimIndent())
    }
}
