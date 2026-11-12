plugins {
    id("entrix.android.library")
}

android {
    namespace = "com.apexbytestudio.entrix.core.export"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
