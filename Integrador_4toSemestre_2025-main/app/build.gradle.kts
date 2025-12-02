plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sistema_riesgos"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.sistema_riesgos"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // --- DEPENDENCIAS PARA RISKGUARD ---
// 1. Red (RF03 - Facade/Decorator)
// Retrofit: Cliente HTTP para llamadas API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
// Gson Converter: Para mapear JSON a Modelos Java (Adapter Pattern)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
// OkHttp: Cliente subyacente, útil para Interceptores (Decorator Pattern)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
// Logging Interceptor: Para el Decorator (opcional, pero muy útil)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
// 2. Arquitectura (MVVM/Observer/State)
// LiveData y ViewModel (para la gestión de estado y Observer)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
// 3. Sensores (RF01 y RF04 - Facade)
// Localización (Geolocator)
    implementation("com.google.android.gms:play-services-location:21.0.1")
// Cámara (CameraX es la moderna, pero usaremos la base de Android)
// Si necesitas CameraX: implementation 'androidx.camera:camera-core:1.3.1'
// 4. Mapa (RF04)
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    implementation("org.osmdroid:osmdroid-android:6.1.14")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.work:work-runtime:2.9.0")
    
    // ML Kit Image Labeling
    implementation("com.google.mlkit:image-labeling:17.0.7")
}