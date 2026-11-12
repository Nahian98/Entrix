plugins {
    id("entrix.android.feature")
}

android {
    namespace = "com.apexbytestudio.entrix.feature.office"
}

dependencies {
    // Map picker for selecting an office location (Maps SDK for Android — map display is free).
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
