plugins { id("com.android.application") }

android {
    namespace = "com.xilitv.ibo"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.xilitv.ibo"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-cleanroom"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
}
