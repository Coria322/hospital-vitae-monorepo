plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Backend local: 10.0.2.2 desde el emulador; en teléfono físico usar `adb reverse tcp:8080 tcp:8080`
// y -Pvitae.apiUrl=http://localhost:8080/api/v1/
val apiUrlDebug = providers.gradleProperty("vitae.apiUrl").getOrElse("http://10.0.2.2:8080/api/v1/")

// Solo debug: -Pvitae.permitirCapturas=true quita FLAG_SECURE para poder hacer capturas al desarrollar. El release nunca.
val permitirCapturasDebug = providers.gradleProperty("vitae.permitirCapturas").getOrElse("false").toBoolean()

// -Pvitae.backend=simulado usa los datos simulados de debug (lo que necesitan las pruebas instrumentadas); por omisión
// debug y release hablan con el backend real (la URL sale de vitae.apiUrl en debug).
val backendRealDebug = providers.gradleProperty("vitae.backend").getOrElse("real") != "simulado"

// Solo para pruebas: -Pvitae.minificarDebug=true pasa el debug por R8 para ver en el teléfono que las reglas keep bastan.
// El release no necesita nada: el release siempre se minifica.
val minificarDebug = providers.gradleProperty("vitae.minificarDebug").getOrElse("false").toBoolean()

android {
    namespace = "com.example.vitae_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.vitae_mobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiUrlDebug\"")
            buildConfigField("String", "API_PINES", "\"\"")
            buildConfigField("boolean", "PERMITIR_CAPTURAS", "$permitirCapturasDebug")
            buildConfigField("boolean", "BACKEND_REAL", "$backendRealDebug")
            enableUnitTestCoverage = true
            optimization {
                enable = minificarDebug
            }
        }
        release {
            // ponytail: servidor de producción actual; RNF-003 exige los pines SHA-256 (sha256/..., separados por
            // coma) del certificado del servidor definitivo. Sin pines, el release se niega a crear el cliente.
            buildConfigField("String", "API_BASE_URL", "\"https://auditoria-backend-u47a.onrender.com/api/v1/\"")
            buildConfigField("String", "API_PINES", "\"\"")
            buildConfigField("boolean", "PERMITIR_CAPTURAS", "false")
            buildConfigField("boolean", "BACKEND_REAL", "true")
            // PROV-SEC-011: ofuscación y reducción con R8 (reglas en src/main/keepRules).
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.zxing.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}