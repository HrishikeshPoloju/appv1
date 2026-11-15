#!/bin/bash

mkdir -p apps/android/gradle
mkdir -p apps/android/app/src/main/java/com/schoolapp/android/ui/theme
mkdir -p apps/android/app/src/main/res/values

cat << 'INNER_EOF' > apps/android/settings.gradle.kts
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SchoolApp"
include(":app")
INNER_EOF

cat << 'INNER_EOF' > apps/android/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
INNER_EOF

cat << 'INNER_EOF' > apps/android/gradle/libs.versions.toml
[versions]
agp = "8.7.3"
kotlin = "2.0.0"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.11.00"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.schoolapp.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.schoolapp.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".SchoolApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SchoolApp"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SchoolApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
INNER_EOF

mkdir -p apps/android/app/src/main/res/xml
cat << 'INNER_EOF' > apps/android/app/src/main/res/xml/backup_rules.xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content xmlns:android="http://schemas.android.com/apk/res/android">
    <include domain="sharedpref" path="."/>
    <exclude domain="sharedpref" path="device.xml"/>
</full-backup-content>
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/res/xml/data_extraction_rules.xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="."/>
        <exclude domain="sharedpref" path="device.xml"/>
    </cloud-backup>
    <device-transfer>
        <include domain="sharedpref" path="."/>
        <exclude domain="sharedpref" path="device.xml"/>
    </device-transfer>
</data-extraction-rules>
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/res/values/strings.xml
<resources>
    <string name="app_name">SchoolApp</string>
</resources>
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/res/values/themes.xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.SchoolApp" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
INNER_EOF

mkdir -p apps/android/app/src/main/res/mipmap-anydpi-v26
cat << 'INNER_EOF' > apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@android:color/white"/>
    <foreground android:drawable="@android:drawable/sym_def_app_icon"/>
</adaptive-icon>
INNER_EOF
cp apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml

cat << 'INNER_EOF' > apps/android/app/src/main/java/com/schoolapp/android/SchoolApplication.kt
package com.schoolapp.android

import android.app.Application

class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/java/com/schoolapp/android/ui/theme/Theme.kt
package com.schoolapp.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SchoolAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
INNER_EOF

cat << 'INNER_EOF' > apps/android/app/src/main/java/com/schoolapp/android/MainActivity.kt
package com.schoolapp.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.schoolapp.android.ui.theme.SchoolAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolAppTheme {
                MainScreen(
                    onOpenChatGPT = { openChatGPT() }
                )
            }
        }
    }

    private fun openChatGPT() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://chatgpt.com/")
        }
        startActivity(intent)
    }
}

@Composable
fun MainScreen(onOpenChatGPT: () -> Unit) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "My Class", "Doubts", "Forms")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.List, Icons.Filled.Person, Icons.Filled.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedItem) {
                0 -> Text("Home Feed (Clean & Intuitive)")
                1 -> Text("My Class")
                2 -> Button(onClick = onOpenChatGPT) { Text("Solve with ChatGPT") }
                3 -> Text("Forms")
            }
        }
    }
}
INNER_EOF

