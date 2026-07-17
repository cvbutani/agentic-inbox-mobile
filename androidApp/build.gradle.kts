import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sonicstarsolutions.agentic.inbox.androidApp"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "com.sonicstarsolutions.agentic.inbox.androidApp"

        versionCode = (project.properties["versionCode"] as? String)?.toInt() ?: 1
        versionName = project.properties["versionName"] as? String ?: "1.0.0"
    }

    // Release signing is only configured when the CI keystore env vars are present,
    // so local debug builds keep working without a keystore.
    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.koin.core)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktorfit)
}
