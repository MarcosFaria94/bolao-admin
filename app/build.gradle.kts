import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// No GitHub Actions estes valores vêm do workflow (contagem de commits).
val codigoVersao = (System.getenv("VERSION_CODE") ?: "1").toInt()
val nomeVersao = System.getenv("VERSION_NAME") ?: "0.0.1-local"
val arquivoChave: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "io.github.marcosfaria94.bolao"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.marcosfaria94.bolao"
        minSdk = 29
        targetSdk = 36
        versionCode = codigoVersao
        versionName = nomeVersao
        buildConfigField("String", "REPO_GITHUB", "\"MarcosFaria94/bolao-admin\"")
    }

    signingConfigs {
        create("release") {
            if (arquivoChave != null) {
                storeFile = file(arquivoChave)
                storeType = "pkcs12"
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "bolao"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (arquivoChave != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.mlkit.texto)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
