import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Cobertura de las pruebas unitarias con Kover (JetBrains)
    alias(libs.plugins.kover)
}

android {
    namespace = "cl.duoc.huertoescolar"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cl.duoc.huertoescolar"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Firma del APK de release con la llave keystore/huerto-escolar.jks
    // (datos en keystore.properties, en la raiz del proyecto).
    val propiedadesFirma = Properties().apply {
        val archivo = rootProject.file("keystore.properties")
        if (archivo.exists()) archivo.inputStream().use { load(it) }
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile", "keystore/huerto-escolar.jks"))
            storePassword = propiedadesFirma.getProperty("storePassword")
            keyAlias = propiedadesFirma.getProperty("keyAlias")
            keyPassword = propiedadesFirma.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Carpeta donde Room deja el esquema exportado de la base de datos.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Carga de imagenes con Coil (AsyncImage), como se vio en clase
    implementation(libs.coil.compose)

    // Consumo de los microservicios y de la API externa de clima (como en el ejemplo del docente)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Persistencia local con Room (SQLite): copia local de los datos de los microservicios

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// ------------------------------------------------------------------------------------
// Cobertura de pruebas unitarias con Kover (herramienta de JetBrains para Kotlin)
//
//   .\gradlew.bat koverHtmlReportDebug -> corre las pruebas y genera el informe en
//                                         app/build/reports/kover/htmlDebug/index.html
//   .\gradlew.bat koverVerifyDebug     -> falla si la cobertura de lineas del codigo
//                                         logico baja del 90 %
//
// Se mide el "codigo logico" que pide la rubrica: ViewModels, repositorios, validaciones,
// reglas del dominio, mapeadores y utilidades. Se excluye lo que no es logica que se
// pueda probar con JUnit en el PC: pantallas Compose, tema, animaciones, navegacion,
// clases generadas por Room/Android, interfaces de Retrofit y modelos de datos (DTO,
// entidades y consultas de Room, que solo guardan valores).
// ------------------------------------------------------------------------------------
kover {
    reports {
        filters {
            excludes {
                // Funciones de interfaz grafica (se revisan en el emulador, no con JUnit)
                annotatedBy("androidx.compose.runtime.Composable")
                classes(
                    // Generado por Android, Room y Compose
                    "*.R", "*.R$*", "*.BuildConfig", "*.Manifest*",
                    "*_Impl", "*_Impl$*", "*ComposableSingletons*",
                    // Pantallas y componentes visuales
                    "cl.duoc.huertoescolar.ui.*Screen*",
                    "cl.duoc.huertoescolar.ui.clima.ComponentesClima*",
                    "cl.duoc.huertoescolar.ui.sincronizacion.AvisoSinConexion*",
                    "cl.duoc.huertoescolar.MainActivity*",
                    // Arranque de la app y acceso a archivos del telefono
                    "cl.duoc.huertoescolar.HuertoApp*",
                    "cl.duoc.huertoescolar.Contenedor*",
                    "cl.duoc.huertoescolar.core.Imagenes*",
                    // Configuracion de red y base de datos Room (sin logica propia)
                    "cl.duoc.huertoescolar.data.remote.RetrofitClient*",
                    "cl.duoc.huertoescolar.data.remote.ApiConfig*",
                    "cl.duoc.huertoescolar.data.local.HuertoDatabase*"
                )
                packages(
                    "cl.duoc.huertoescolar.ui.theme",
                    "cl.duoc.huertoescolar.ui.componentes",
                    "cl.duoc.huertoescolar.ui.animaciones",
                    "cl.duoc.huertoescolar.ui.navegacion",
                    "cl.duoc.huertoescolar.data.remote.api",
                    "cl.duoc.huertoescolar.data.remote.dto",
                    "cl.duoc.huertoescolar.data.local.dao",
                    "cl.duoc.huertoescolar.data.local.entity",
                    "cl.duoc.huertoescolar.data.local.query"
                )
            }
        }
        verify {
            rule("Cobertura minima de lineas del codigo logico") {
                minBound(90)
            }
        }
    }
}
