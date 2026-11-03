plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "dev.andoshin11.agentstatuspanel"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.andoshin11.agentstatuspanel"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")
    testImplementation("junit:junit:4.13.2")
}
