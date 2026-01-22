import java.util.Properties

rootProject.name = "ARK-Drop"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
if (rootProject.projectDir.resolve("local.properties").exists()) {
    localProperties.load(rootProject.projectDir.resolve("local.properties").inputStream())
}
val githubToken = localProperties.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN")
?: throw IllegalStateException("GITHUB_TOKEN not found")

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            name = "ark-core GitHub Packages"
            url = uri("https://maven.pkg.github.com/Ark-Builders/ark-core")
            credentials {
                username = "token"
                password = githubToken
            }
        }
    }
}

include(":composeApp")
include(":shared")