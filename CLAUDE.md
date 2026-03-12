# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and install debug APK:
```
./gradlew assembleDebug
```

Run unit tests:
```
./gradlew test
```

Run a single test class:
```
./gradlew test --tests "com.ashleykaminski.aipackinglist.PackingListViewModelTest"
```

Run a single test by name:
```
./gradlew test --tests "com.ashleykaminski.aipackinglist.PackingListViewModelTest.initial state is correct"
```

Run instrumented (on-device) tests:
```
./gradlew connectedAndroidTest
```

## Architecture

This is a single-Activity Jetpack Compose app using a manual navigation pattern (no Jetpack Navigation library). Navigation state is a `Screen` sealed class held in `rememberSaveable` in `PackingListApp` (MainActivity.kt).

**Data layer:** All state lives in a single `UserPreferences` protobuf DataStore (`user_prefs.pb`). `UserPreferencesSerializer` handles JSON serialization via `kotlinx.serialization`. The DataStore is accessed via a `Context` extension property `userPreferencesDataStore` defined in `UserPreferences.kt`.

**ViewModel:** `PackingListViewModel` holds a `StateFlow<UserPreferences>` and exposes discrete mutation functions (add list, rename list, update items, etc.). It is created via `PackingListViewModelFactory` which injects the DataStore. All mutations go through `dataStore.updateData {}`.

**Screens:**
- `AllPackingListsScreen` — shows all packing lists, supports add/rename
- `SelectableListScreen` — shows items within a single list, supports add/check/rename

**Testing:** Unit tests use `InMemoryUserPreferencesDataStore` (a fake DataStore), `MainCoroutineRule` (swaps the main dispatcher), Turbine for Flow testing, and Truth for assertions.
