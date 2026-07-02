plugins {
    id("entrix.jvm.library")
}

// Shared test infrastructure. Exposes the common test libraries as `api` so consuming modules only
// need `testImplementation(project(":core:testing"))`.
dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
}
