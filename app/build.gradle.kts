import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.crownos.connect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.crownos.connect"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    packaging {
        jniLibs { useLegacyPackaging = false }
    }

    // Only needed to cross-compile llts-android (`-Pllts.cargoNdk=true`); arm64 only, since every
    // device CrownConnect targets is arm64 and each extra ABI is another full engine build.
    ndkVersion = providers.gradleProperty("llts.ndkVersion").getOrElse("27.2.12479018")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

/**
 * Cross-compiles the llts engine (`llts-protocol/crates/llts-android`) into jniLibs.
 *
 * Off by default so the app builds without an NDK or a Rust toolchain; `just android` in
 * llts-protocol drops a prebuilt `.so` into `src/main/jniLibs` instead. Use one or the other, so
 * it is clear which build gets packaged. A task type of its own rather than a bare `Exec`, so the
 * configuration cache holds: the command never reaches back into the project.
 */
@CacheableTask
abstract class CargoNdkBuild : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val crates: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val lockfile: RegularFileProperty

    @get:Input
    abstract val abi: Property<String>

    @get:Input
    abstract val apiLevel: Property<String>

    @get:Internal
    abstract val workspace: DirectoryProperty

    @get:Internal
    abstract val ndkDirectory: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun build() {
        execOperations.exec {
            workingDir = workspace.get().asFile
            environment("ANDROID_NDK_HOME", ndkDirectory.get())
            commandLine(
                "cargo", "ndk",
                "--target", abi.get(),
                "--platform", apiLevel.get(),
                "--output-dir", outputDirectory.get().asFile.absolutePath,
                "build", "--profile", "android", "-p", "llts-android",
            )
        }
    }
}

if (providers.gradleProperty("llts.cargoNdk").map(String::toBoolean).getOrElse(false)) {
    val llts = layout.projectDirectory.dir(
        providers.gradleProperty("llts.workspace").getOrElse("../../llts-protocol"),
    )
    val cargoNdk = tasks.register<CargoNdkBuild>("cargoNdkBuild") {
        group = "build"
        description = "Cross-compiles the llts engine for Android"
        crates.set(llts.dir("crates"))
        manifest.set(llts.file("Cargo.toml"))
        lockfile.set(llts.file("Cargo.lock"))
        workspace.set(llts)
        ndkDirectory.set(androidComponents.sdkComponents.ndkDirectory.map { it.asFile.absolutePath })
        abi.set("arm64-v8a")
        apiLevel.set("29")
        outputDirectory.set(layout.buildDirectory.dir("rustJniLibs"))
    }
    androidComponents {
        onVariants { variant ->
            variant.sources.jniLibs?.addGeneratedSourceDirectory(cargoNdk, CargoNdkBuild::outputDirectory)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    implementation(libs.zxing.core)
    implementation(libs.lucide.icons)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
