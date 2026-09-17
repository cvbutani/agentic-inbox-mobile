import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.sonicstarsolutions.agentic.inbox.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Macrobenchmark's own requirement, independent of androidApp's minSdk = 24.
        minSdk = 28
        targetSdk = 37
    }

    targetProjectPath = ":androidApp"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// Only ever build/run instrumented against release, since that's the variant whose
// startup/rendering profile actually ships to users.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
}
