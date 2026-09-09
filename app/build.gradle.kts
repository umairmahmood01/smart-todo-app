import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Reads the gitignored `local.properties`, or an empty set of properties when the file is
 * absent (fresh clone, CI checkout).
 */
val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

/**
 * Resolves a build-time secret: `local.properties` first (developer machines), then an
 * environment variable (CI), then the empty string.
 *
 * The empty default is deliberate. Every consumer treats a blank value as "feature not
 * configured" and degrades silently, so a clone with no `local.properties` still builds and
 * still runs - it just never calls the remote enrichment service.
 */
fun secret(propertyKey: String, environmentKey: String): String =
    (localProperties.getProperty(propertyKey) ?: System.getenv(environmentKey) ?: "").trim()

/** Renders [value] as a quoted Kotlin string literal safe to paste into `BuildConfig`. */
fun stringLiteral(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"" + escaped + "\""
}

android {
    namespace = "com.umair.smarttodo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.umair.smarttodo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- Remote enrichment (optional feature) -------------------------------------
        //
        // SECURITY, stated plainly: ENRICHMENT_FUNCTION_KEY is compiled into the APK. Anyone
        // who can read the binary can extract it - `strings`, `apktool` or a debugger will
        // all reveal it, and no amount of obfuscation changes that. It is a throttling
        // credential, not a secret. The real mitigations live server-side: the Azure Function
        // enforces a per-key rate limit, the key is scoped to this single function, and it can
        // be rotated without shipping a new client. Never put a credential here that grants
        // anything beyond calling this one endpoint.
        buildConfigField(
            "String",
            "ENRICHMENT_BASE_URL",
            stringLiteral(secret("enrichment.baseUrl", "ENRICHMENT_BASE_URL")),
        )
        buildConfigField(
            "String",
            "ENRICHMENT_FUNCTION_KEY",
            stringLiteral(secret("enrichment.functionKey", "ENRICHMENT_FUNCTION_KEY")),
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    testOptions {
        unitTests {
            // Stubbed android.jar methods return 0/null instead of throwing, so a stray
            // framework call in a JVM test surfaces as a normal assertion failure.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
    testImplementation(libs.kotlinx.serialization.json)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
