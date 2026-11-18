import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gr8eam.vaktpost"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gr8eam.vaktpost"
        minSdk = 26
        targetSdk = 36
        // CI skickar -PbuildNumber=<körningsnummer>; testbyggen får då versionen 0.1.0-test.<n>.
        // Bygget för Google Play (-PplayRelease) får versionsnamnet playVersion; versionskoden är alltid körningsnumret.
        val baseVersion = "0.1.0"
        val playVersion = "1.0.0"
        val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull()
        versionCode = buildNumber ?: 1
        versionName = if (project.hasProperty("playRelease")) playVersion
            else if (buildNumber != null) "$baseVersion-test.$buildNumber" else "$baseVersion-dev"
    }

    signingConfigs {
        // Testnyckel (publik med flit) så att varje CI-bygge kan installeras ovanpå det förra.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Riktiga utgåvor signeras med nyckel från GitHub secrets (se build.yml).
        create("release") {
            val ksPath = System.getenv("RELEASE_KEYSTORE")
            if (ksPath != null && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug" // testappen kan ligga bredvid den riktiga utgåvan
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        getByName("main").kotlin.srcDirs("src/main/kotlin")
    }
    lint {
        abortOnError = false
    }
    // android-mail och android-activation har egna licensfiler i META-INF som annars krockar.
    // Javamails tjänstefiler (META-INF/javamail.*, META-INF/services) måste däremot följa med.
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE",
                "META-INF/NOTICE.txt", "META-INF/DEPENDENCIES"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    // Valfri e-postsändning via SMTP (Jakarta/JavaMail-porten för Android). Används bara om användaren fyllt i ett konto.
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}
