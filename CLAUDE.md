# Entrix — Project Instructions

## Architecture (do not break)
- Clean Architecture + MVVM, enforced by Gradle modules. Dependencies point INWARD only.
- `:domain` is a pure-JVM module (`entrix.jvm.library`). NEVER add an Android dependency to it.
- Features depend on `:domain` interfaces only — never on `:data`. Only `:app` depends on `:data`.
- Interfaces (repositories, GeofenceScheduler, LocationProvider, TimesheetExporter) live in
  `:domain`; implementations live in `:data` / `:core:*`. Add new impls bound to interfaces, don't
  have features reach past the interface.
- Models: cross-layer models in `:core:model`; domain-only aggregates in `:domain/model`.
- Use cases are stateless; register them as Koin `factory`. Repositories/data sources are `single`.

## DI — Koin (NOT Hilt)
- Each module owns a `*Module` Koin module; `:app` assembles them in `EntrixApplication`.
- When adding a definition, also add it to the correct module AND keep `KoinGraphTest` green —
  run `:app:testDebugUnitTest` (KoinGraphTest) after any DI change; it verifies the whole graph.
- Bind impls with `singleOf(::Impl) bind Interface::class`; ViewModels with `viewModelOf(::Vm)`.

## Build system
- Module build files must stay minimal — shared config lives in `build-logic` convention plugins
  (`entrix.android.feature`, `entrix.jvm.library`, `entrix.android.room`, etc.). Add dependencies to
  the convention plugin when they're needed by every module of a kind; otherwise to the module.
- Versions come from `gradle/libs.versions.toml` only. compileSdk 36 / minSdk 28 / JVM 17.
- AGP 9: keep `android.disallowKotlinSourceSets=false` (Room/KSP source path). Set JAVA_HOME to
  Android Studio's JBR when invoking Gradle. Configuration cache is on.
- Do NOT run `clean`.

## High-risk areas
- Location/geofencing lives in `:core:location` and owns its own manifest (location + boot
  permissions, unexported receivers). Respect background-location limits and boot re-registration.
- Room schema changes: `:core:database` exports schemas to `schemas/` — add a migration and update
  the exported schema; never bump version without one.
- Release signing uses git-ignored `keystore.properties` (fallback to debug). Never commit it or
  weaken signing/R8.
- Navigation is type-safe `@Serializable` routes in `:core:navigation` — no string routes.

## Testing
- Use `:core:testing` (`testImplementation(project(":core:testing"))`) — junit, coroutines-test,
  turbine, mockk, MainDispatcherRule.
- ViewModels: mockk the use case, assert on `uiState` with turbine.
- Prefer the narrowest module test; then the relevant module compile; KoinGraphTest for DI changes.
