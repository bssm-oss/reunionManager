import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun loadDotEnv(rootDir: File): Map<String, String> {
    val dotEnv = rootDir.resolve(".env")
    if (!dotEnv.isFile) return emptyMap()
    return dotEnv.readLines()
        .asSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotBlank() && !line.startsWith("#") && line.contains("=") }
        .map { line ->
            line.substringBefore("=")
                .removePrefix("export")
                .trim() to line.substringAfter("=")
        }
        .associate { (key, value) -> key.trim() to value.trim().trim('"', '\'') }
}

fun buildConfigString(value: String): String {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""
}

val dotEnv = loadDotEnv(rootProject.projectDir)
val openRouterApiKey = (
    providers.environmentVariable("OPENROUTER_API_KEY").orNull
        ?: dotEnv["OPENROUTER_API_KEY"]
        ?: ""
).trim()

android {
    namespace = "com.bssm.reunionmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bssm.reunionmanager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "OPENROUTER_MODEL", buildConfigString("deepseek/deepseek-v4-flash"))
        buildConfigField("String", "OPENROUTER_API_KEY", buildConfigString(""))
    }

    buildTypes {
        debug {
            buildConfigField("String", "OPENROUTER_API_KEY", buildConfigString(openRouterApiKey))
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "OPENROUTER_API_KEY", buildConfigString(""))
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

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        warningsAsErrors = true
        xmlReport = true
        lintConfig = file("../lint.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.jvm.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.android.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.google.ai.edge.litertlm.android)

    testImplementation(libs.junit4)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.uiautomator)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
