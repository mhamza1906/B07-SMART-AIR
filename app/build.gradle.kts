plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.googleServices)


}

android {
    namespace = "com.example.smart_air"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.smart_air"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
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
    implementation(platform(libs.firebase.bom))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.google.firebase.firestore)
    implementation(libs.recyclerview)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)


    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    // Android Instrumentation Tests
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide（普通方式，不走 catalog）
    //noinspection UseTomlInstead
    implementation("com.github.bumptech.glide:glide:5.0.5")
    //noinspection UseTomlInstead
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")
}
