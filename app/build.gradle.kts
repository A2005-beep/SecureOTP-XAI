plugins {
    alias(libs.plugins.android.application)
}

android {

    namespace = "com.example.secureotp_xai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.secureotp_xai"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.tensorflow:tensorflow-lite:2.10.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.10.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Room Database Components
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230227")

    // ViewModel and LiveData Components
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.6")

}