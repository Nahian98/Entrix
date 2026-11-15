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
- Geofence → attendance policy lives in `RecordGeofenceTransitionUseCase` (domain): ENTER caches a
  pending time, DWELL (120s loitering) opens the visit back-dated to it, EXIT closes it, and an
  ENTER/DWELL within `AttendanceRules.EXIT_GRACE_PERIOD` (10 min) of a check-out *reopens* that visit
  (false EXITs from coarse indoor fixes). `ReconcileOpenVisitsUseCase` caps open visits at 16h and
  capped visits are never reopened. Any policy change needs cases in `RecordGeofenceTransitionUseCaseTest`.

## UI conventions
- Theme: `EntrixTheme` uses the Entrix emerald palette by default (`dynamicColor = false`, Material You
  is opt-in). `Color.kt` sets EVERY role incl. `surfaceContainer*` — keep it that way when adding
  roles. Features use `MaterialTheme.colorScheme` only; no hardcoded colors (the nav bar's monochrome
  `EntrixNavigationBarDefaults` is the one exception).
- App shell (`:app/ui`): floating `EntrixNavigationBar` + collapsing `EntrixTopAppBar`, titles/actions
  per route in `EntrixTopBar`. Content draws behind the nav bar: every screen takes
  `contentPadding: PaddingValues` and applies it INSIDE its scroll container (after `verticalScroll` /
  as `LazyColumn` contentPadding), never as an outer padding.
- Header actions needing screen state read the same ViewModel via
  `koinViewModel(viewModelStoreOwner = backStackEntry)`. Dialogs/side effects stay in the feature;
  hoist only visibility (see History "Clear history", Reports export).
- Reuse `:core:designsystem` pieces before writing new ones: `EntrixStatTile`, `EntrixToggle`,
  `EntrixBarChart`, `EntrixTopAppBar`, and `EntrixIcons` (custom 24dp, 1.5-stroke line icons; filled
  variants cut details as even-odd holes so one tint works). Add icons there — don't pull in
  material-icons-extended. Screen pattern: section title above a rounded (24dp) `surfaceContainer`
  group of rows with inset dividers.

## Time & live values
- `java.time.Clock` is provided by Koin (`factory` in `domainModule`); inject it — no `Instant.now()`
  / `LocalDate.now()` in ViewModels or use cases. Tests use `Clock.fixed` or a scheduler-backed clock.
- "Time so far" for an open visit ticks via `minuteTicks(clock)` (`:domain/time`), only while an open
  visit exists and under `SharingStarted.WhileSubscribed`. Home, History and Reports all count open
  visits up to now; the PDF timesheet counts completed visits only.

## Devices
- A physical test phone (Xiaomi/HyperOS) is often attached: set `ANDROID_SERIAL=emulator-5554` so
  `installDebug` / connected tests don't hit it. On the Xiaomi use plain `adb install -r` (no input
  injection / grant-all). The emulator is a dual-display foldable: `screencap -d <display id>`.

## Testing
- Use `:core:testing` (`testImplementation(project(":core:testing"))`) — junit, coroutines-test,
  turbine, mockk, MainDispatcherRule.
- ViewModels: mockk the use case, assert on `uiState` with turbine.
- Prefer the narrowest module test; then the relevant module compile; KoinGraphTest for DI changes.
