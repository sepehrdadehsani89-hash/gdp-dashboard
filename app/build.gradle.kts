import java.security.KeyStore
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "26.3.11579264"
    defaultConfig {
        applicationId = "com.jarvis.next"
        minSdk = 29
        targetSdk = 35
        versionCode = 19
        versionName = "11.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        ndk { abiFilters += providers.gradleProperty("jarvisAbis").getOrElse("arm64-v8a").split(",") }
    }
    buildFeatures { compose = true; buildConfig = true }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        create("reinstall") {
            val privateDir = providers.environmentVariable("JARVIS_REINSTALL_SIGNING_DIR").orNull
            if (privateDir != null) {
                storeFile = file("$privateDir/jarvis-reinstall.p12")
                storePassword = file("$privateDir/store-password").readText().trim()
                keyAlias = "jarvis-reinstall"
                keyPassword = storePassword
                storeType = "PKCS12"
            }
        }
        create("next") {
            val privateDir = providers.environmentVariable("JARVIS_NEXT_SIGNING_DIR").orNull
            if (privateDir != null) {
                storeFile = file("$privateDir/jarvis-next.p12")
                storePassword = file("$privateDir/store-password").readText().trim()
                keyAlias = "jarvis-next"
                keyPassword = storePassword
                storeType = "PKCS12"
            }
        }
    }
    buildTypes {
        create("reinstall") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("reinstall")
            isDebuggable = false
            isMinifyEnabled = false
            versionNameSuffix = "-reinstall"
            matchingFallbacks += listOf("release")
        }
        release {
            signingConfig = signingConfigs.getByName("next")
            // JNI libraries use named Kotlin/Java fields and callbacks. Preserve their ABI.
            // This signed release is intentionally unminified, like the proven development build.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Smaller sideload download; Android extracts native code during installation.
        jniLibs.useLegacyPackaging = true
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = true }
}

dependencies {
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    debugImplementation(libs.compose.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.coroutines)
    implementation(libs.vosk)
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(files("libs/sherpa-onnx-1.12.15.aar"))
    implementation(files("libs/tesseract4android-4.8.0.aar"))
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

val checkNextSigning by tasks.registering {
    doLast {
        val privateDir = providers.environmentVariable("JARVIS_NEXT_SIGNING_DIR").orNull
            ?: error("Set JARVIS_NEXT_SIGNING_DIR to the private signing directory. Never generate a replacement key for an existing Jarvis Next installation.")
        check(file("$privateDir/jarvis-next.p12").isFile && file("$privateDir/store-password").isFile) { "Original Jarvis Next signing files are missing." }
        val password = file("$privateDir/store-password").readText().trim().toCharArray()
        try {
            val keys = KeyStore.getInstance("PKCS12")
            file("$privateDir/jarvis-next.p12").inputStream().use { keys.load(it, password) }
            check(keys.isKeyEntry("jarvis-next")) { "The original signing alias is unavailable." }
            val certificate = keys.getCertificate("jarvis-next") ?: error("Signing certificate is missing.")
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString("") { "%02x".format(it) }
            check(fingerprint == "56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719") {
                "Signing certificate does not match the installed Jarvis identity. Refusing to sign."
            }
        } finally { password.fill('\u0000') }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkNextSigning) }

val checkReinstallSigning by tasks.registering {
    doLast {
        val privateDir = providers.environmentVariable("JARVIS_REINSTALL_SIGNING_DIR").orNull
            ?: error("Set JARVIS_REINSTALL_SIGNING_DIR. Reinstall builds cannot update the old Jarvis installation.")
        val password = file("$privateDir/store-password").readText().trim().toCharArray()
        try {
            val keys = KeyStore.getInstance("PKCS12")
            file("$privateDir/jarvis-reinstall.p12").inputStream().use { keys.load(it, password) }
            check(keys.isKeyEntry("jarvis-reinstall")) { "Retained reinstall signing alias is unavailable." }
            val certificate = keys.getCertificate("jarvis-reinstall") ?: error("Signing certificate is missing.")
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString("") { "%02x".format(it) }
            check(fingerprint == "1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda") {
                "Wrong reinstall identity. Refusing to sign."
            }
        } finally { password.fill('\u0000') }
    }
}
tasks.matching { it.name == "preReinstallBuild" }.configureEach { dependsOn(checkReinstallSigning) }
