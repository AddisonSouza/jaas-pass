plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jaaspass"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jaaspass"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// CONSTRAINT (design §1): NENHUMA dependência de terceiros.
// Apenas o SDK Android (android.*, java.*, javax.crypto.*) e a Kotlin stdlib (via plugin).
// Dependências de teste, quando necessárias (tasks §1.5), serão discutidas em /ps:apply.
dependencies {
}
