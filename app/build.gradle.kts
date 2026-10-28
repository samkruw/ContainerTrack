import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}
fun publicSetting(name: String) = providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get()
fun javaLiteral(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "ch.zerogen.containertrack"
    compileSdk = 36
    defaultConfig {
        applicationId = "ch.zerogen.containertrack"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.5.0"
        for (part in listOf("TENANT_ID", "CLIENT_ID", "REDIRECT_URI", "TEAM_ID", "CHANNEL_ID")) {
            buildConfigField("String", "MICROSOFT_$part", javaLiteral(publicSetting("CT_MICROSOFT_$part")))
        }
        manifestPlaceholders["msalRedirectPath"] = runCatching { URI(publicSetting("CT_MICROSOFT_REDIRECT_URI")).path }
            .getOrNull()?.takeIf { it.startsWith("/") && it.length > 1 } ?: "/not-configured"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions { unitTests.isReturnDefaultValues = true }
}
room { schemaDirectory("$projectDir/schemas") }
dependencies {
    // Align the app and Room migration-test APK; mixed serialization ABIs fail at runtime.
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation(libs.msal)
    implementation(libs.work)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit)
    implementation(libs.exif)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.json.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
