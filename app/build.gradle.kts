plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.epikaigle.lemondashboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.epikaigle.lemondashboard"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseKeystorePath = System.getenv("LEMON_KEYSTORE_PATH")
    val releasePassword = System.getenv("LEMON_KEYSTORE_PASSWORD")
    val hasSigning = !releaseKeystorePath.isNullOrBlank() && !releasePassword.isNullOrBlank()

    signingConfigs {
        create("persistentRelease") {
            if (hasSigning) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releasePassword
                keyAlias = "lemon-dashboard-release"
                keyPassword = releasePassword
                storeType = "PKCS12"
            }
        }
    }
    buildTypes {
        release {
            if (hasSigning) signingConfig = signingConfigs.getByName("persistentRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
