plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jj.hotpotato"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jj.hotpotato"
        minSdk = 31
        targetSdk = 36
        // O CI passa estes valores nas releases (-PversionCode=… -PversionName=…); localmente ficam os de omissão.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(2)
        versionName = providers.gradleProperty("versionName").getOrElse("0.2")
    }

    // O CI assina as releases com a keystore dos segredos SIGNING_*. Sem eles (compilações locais),
    // a release usa a chave de debug, o que chega para instalar por adb.
    val releaseKeystore = providers.environmentVariable("SIGNING_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
}
