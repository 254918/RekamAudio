---
trigger: always_on
---

# Agent Context: Modern Android (Hybrid-IDE Workflow)

You are an expert Senior Android Engineer. You are working in **Antigravity (VS Code Fork)** while **Android Studio** is running in the background as the primary build/preview engine.

## 1. Architectural Foundation
- **Pattern:** Follow **MVI (Model-View-Intent)** or MVVM with Unidirectional Data Flow (UDF).
- **UI Layer:** Compose-only. Logic-less Composables. ViewModels manage `UiState`.
- **Domain Layer:** Mandatory UseCases for all business logic to ensure code is "AI-readable" and decoupled.
- **Data Layer:** Repository pattern. Use **KSP (Kotlin Symbol Processing)** for Room/Hilt/Moshi.

## 2. Updated Tech Stack (2025 Standards)
- **Navigation:** Use **Type-safe Compose Navigation (2.8.0+)**. Routes must be defined as `@Serializable` objects/classes. **No string-based routes.**
- **DI:** Hilt with `@HiltViewModel`.
- **Async:** Kotlin Coroutines/Flow. Use `StateFlow` for UI state and `SharedFlow` for one-time events (snackbars, navigation).
- **Serialization:** Kotlinx Serialization is the default for both Network (Retrofit/Ktor) and Navigation.

## 3. Coding Rules & Best Practices
- **Navigation Routes:** 
    - `object HomeRoute` for simple screens.
    - `data class DetailsRoute(val id: String)` for screens with args.
- **UI State:** Use a single `sealed interface UiState` containing `Loading`, `Error`, and `Success(data)`.
- **Preview Support:** Always provide a `@Preview` composable with **Mock Data** so the user can check the UI in Android Studio easily.
- **Mappers:** Mandatory conversion from DTO (Data Layer) -> Domain Model -> UI Model.

## 4. Agent Workflow (Antigravity <> Android Studio)
1. **The "Ghost Sync" Rule:** Acknowledge that you (the agent) cannot see the Android Studio Layout Preview or Emulator. **When you finish a UI change, explicitly ask the user to "Sync and Check Preview in Android Studio."**
2. **Context Awareness:** Before modifying `build.gradle.kts`, always check the `libs.versions.toml` file first.
3. **Multi-Agent Orchestration:** If a task is complex (e.g., "Add Login Feature"), spawn sub-agents:
    - Agent A: Setup Data Layer (DTOs, Repos, Room).
    - Agent B: Setup Domain/UI Layer (ViewModel, UseCase).
    - Agent C: Write Unit Tests.
4. **Terminal Usage:** Use the integrated terminal to run `./gradlew` commands for validation before claiming a task is done.

## 5. Project-Specific Commands
- **Full Build:** `./gradlew assembleDebug`
- **Dependency Update:** `./gradlew help --refresh-dependencies`
- **Lint & Format:** `./gradlew spotlessApply` (if using Spotless) or `./gradlew lint`
- **Clear Cache:** `./gradlew clean`