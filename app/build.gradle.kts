import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperty("robolectric.graphicsMode", "NATIVE") }
        }
    }

    packaging {
        jniLibs { useLegacyPackaging = false }
    }

    // Only needed to cross-compile the Rust engines (`-Pllts.cargoNdk=true`); arm64 only, since
    // every device CrownConnect targets is arm64 and each extra ABI is another full engine build.
    ndkVersion = providers.gradleProperty("llts.ndkVersion").getOrElse("27.2.12479018")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

/**
 * Cross-compiles one of the app's Rust engines into jniLibs: llts (`llts-protocol`) or ffsp
 * (`fileshare/rust`).
 *
 * Off by default so the app builds without an NDK or a Rust toolchain; `just android` in
 * llts-protocol and `cargo ndk` in fileshare/rust drop prebuilt libraries into
 * `src/main/jniLibs` instead. Use one or the other, so it is clear which build gets packaged. A
 * task type of its own rather than a bare `Exec`, so the configuration cache holds: the command
 * never reaches back into the project.
 */
@CacheableTask
abstract class CargoNdkBuild : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val cargoArguments: ListProperty<String>

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
                listOf(
                    "cargo", "ndk",
                    "--target", abi.get(),
                    "--platform", apiLevel.get(),
                    "--output-dir", outputDirectory.get().asFile.absolutePath,
                    "build",
                ) + cargoArguments.get(),
            )
        }
    }
}

if (providers.gradleProperty("llts.cargoNdk").map(String::toBoolean).getOrElse(false)) {
    val llts = layout.projectDirectory.dir(
        providers.gradleProperty("llts.workspace").getOrElse("../../llts-protocol"),
    )
    val fileshare = layout.projectDirectory.dir(
        providers.gradleProperty("ffsp.workspace").getOrElse("../../fileshare/rust"),
    )
    val ffspProtocol = layout.projectDirectory.dir(
        providers.gradleProperty("ffsp.protocol").getOrElse("../../ffsp-protocol"),
    )
    fun registerEngine(name: String, workspaceDirectory: Directory, arguments: List<String>, inputs: List<Any>) =
        tasks.register<CargoNdkBuild>("cargoNdkBuild$name") {
            group = "build"
            description = "Cross-compiles the $name engine for Android"
            sources.from(inputs)
            cargoArguments.set(arguments)
            workspace.set(workspaceDirectory)
            ndkDirectory.set(androidComponents.sdkComponents.ndkDirectory.map { it.asFile.absolutePath })
            abi.set("arm64-v8a")
            apiLevel.set("29")
            outputDirectory.set(layout.buildDirectory.dir("rustJniLibs/${name.lowercase()}"))
        }
    val engines = listOf(
        registerEngine(
            "Llts",
            llts,
            listOf("--profile", "android", "-p", "llts-android"),
            listOf(llts.dir("crates"), llts.file("Cargo.toml"), llts.file("Cargo.lock")),
        ),
        registerEngine(
            "Ffsp",
            fileshare,
            listOf("--release"),
            listOf(fileshare.dir("src"), fileshare.file("Cargo.toml"), ffspProtocol.dir("src")),
        ),
    )
    androidComponents {
        onVariants { variant ->
            engines.forEach { engine ->
                variant.sources.jniLibs?.addGeneratedSourceDirectory(engine, CargoNdkBuild::outputDirectory)
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.process)
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
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
