plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nubiasms.messages"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nubiasms.messages"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = mutableMapOf<String, String>()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.readLines().forEach { line ->
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) return@forEach
                val i = t.indexOf('=')
                if (i > 0) localProps[t.substring(0, i).trim()] = t.substring(i + 1).trim()
            }
        }
        fun localOr(key: String, default: String) =
            localProps[key]?.takeIf { it.isNotBlank() } ?: default
        buildConfigField("String", "UPDATE_GITHUB_OWNER", "\"${localOr("update.github.owner", "YOUR_GITHUB_USER")}\"")
        buildConfigField("String", "UPDATE_GITHUB_REPO", "\"${localOr("update.github.repo", "nubiaSMS")}\"")
        buildConfigField("String", "UPDATE_APK_ASSET", "\"${localOr("update.apk.asset", "nubiasms-release.apk")}\"")
    }

    buildTypes {
        release {
            // R8 + resource shrinking: Compose runs several times faster than in debug.
            isMinifyEnabled = true
            isShrinkResources = true
            // Personal sideload build: sign with the debug key so it installs over debug.
            signingConfig = signingConfigs.getByName("debug")
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

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Installs the Compose libraries' baseline profiles on sideloaded builds (AOT for hot paths).
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
