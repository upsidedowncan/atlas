import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serializationJson)

            implementation(libs.kotlinx.serializationJson)
            implementation(libs.kotlinx.coroutinesCore)

            implementation(libs.compose.icons.material.symbols)
            implementation(libs.compose.icons.material.symbols.outlined)
            implementation(libs.compose.icons.material.symbols.rounded.filled)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kmp.emoji.picker)
            implementation(libs.image.picker.kmp)
            implementation(libs.whyoleg.cryptography.core)
            implementation(libs.markdown.renderer)
            api(libs.compose.webview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.java)
            implementation(libs.whyoleg.cryptography.jdk)
        }

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.whyoleg.cryptography.jdk)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.whyoleg.cryptography.apple)
        }
    }
}

android {
    namespace = "atlas.messenger"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "atlas.messenger"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.0.2.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.resources {
    packageOfResClass = "atlas.messenger.generated.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "atlas.messenger.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "atlas.messenger"
            packageVersion = "0.0.2.1"
        }
    }
}

// hot reload is happier when it gets the jetbrains runtime it expects
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })
}

tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    val userId = project.findProperty("user") as? String ?: "0"
    systemProperty("atlas.user", userId)
    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
    val isMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)
    if (isMac) {
        jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
    }
}
