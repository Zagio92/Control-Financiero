plugins {
    alias(libs.plugins.android.application)
}

// Versión: GitHub Actions la pasa con -PversionCode=N (número de compilación).
// En Android Studio, sin parámetros, se usa 1.
val appVersionCode = (providers.gradleProperty("versionCode").orNull ?: "1").toInt()
val appVersionName = providers.gradleProperty("versionName").orNull ?: "1.0.$appVersionCode"

android {
    namespace = "com.zagio.finanzapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.zagio.finanzapp"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma fija para que cada APK nuevo pueda actualizar al instalado.
    // La clave no está en el repositorio: GitHub Actions la crea desde los secretos.
    val keystorePath = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// La web (index.html, manifest, sw.js, icons) vive en la raíz del repositorio.
// Se copia a los assets de la app al compilar, así solo hay una copia que mantener.
abstract class CopyWebTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val webFiles: ConfigurableFileCollection

    @get:Internal
    abstract val webRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(webRoot) { include(WEB_FILES) }
            into(outputDir)
        }
    }

    companion object {
        val WEB_FILES = listOf("index.html", "manifest.webmanifest", "sw.js", "icons/**")
    }
}

val webRootDir = rootProject.layout.projectDirectory.dir("..")
val copyWeb = tasks.register<CopyWebTask>("copyWeb") {
    webRoot.set(webRootDir)
    webFiles.from(fileTree(webRootDir) { include(CopyWebTask.WEB_FILES) })
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyWeb, CopyWebTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
