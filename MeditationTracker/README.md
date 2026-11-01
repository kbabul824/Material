# Meditation Tracker - Native Android App

A clean, modern native Android meditation app designed with **Material You**, built on **Jetpack Compose**, **Hilt**, **Room**, and a resilient **Foreground Service** to guarantee uninterrupted background meditation timers and interval chimes.

---

## 🌟 Core Features

1. **Interval Reminder Mode**:
   - Customize total meditation duration and bell intervals (e.g., chime every 2, 3, or 5 minutes).
   - Ideal for breath refocusing, postural check-ins, or staged mindfulness practices.

2. **Long Duration Mode**:
   - Set extended meditation sessions (10 to 120 minutes).
   - Optional gentle milestone alert bells at the 25%, 50%, and 75% marks to indicate progress without checking the screen.

3. **Mindful Session Screen**:
   - Animated organic breathing visualizer for guided rhythmic focus.
   - High-contrast countdown timer and simple one-tap Pause, Resume, or Early Finish.
   - Foreground service with notification controls to prevent battery optimizations from killing timers.

4. **Session History & Analytics**:
   - Comprehensive local SQLite storage via Room.
   - Filter by practice mode (All, Interval, Long Duration).
   - Quick statistics: total completed sessions and aggregated mindful hours/minutes.
   - Notes journaling for personal reflection after each session.

5. **Accessibility & Internationalization**:
   - Dynamic Colors (Material You) on Android 12+, with accessible, high-contrast dark and light fallbacks.
   - Scalable typography supporting system font sizing and TalkBack screen reader labels.
   - English and Spanish (`res/values-es/strings.xml`) localization ready out of the box.

---

## 🏗️ Architecture & Package Layout

Built following the official Android Modern App Architecture recommendations (MVVM + Clean Architecture + Repository Pattern):

```text
com.meditation.tracker
├── MeditationApp.kt                # @HiltAndroidApp, creates Notification channels
├── MainActivity.kt                 # Single Activity entry point with Compose
├── data
│   ├── local
│   │   ├── AppDatabase.kt          # Room DB declaration & type converters
│   │   ├── SessionDao.kt           # Reactive Room queries returning Flows
│   │   └── UserPreferencesRepository.kt # DataStore for sounds, vibration & theme
│   ├── model
│   │   ├── MeditationMode.kt       # INTERVAL, LONG_DURATION enum
│   │   ├── ReminderInterval.kt     # Checkpoint offset & label model
│   │   └── SessionHistory.kt       # Persistent Room entity
│   └── repository
│       ├── MeditationRepository.kt     # Clean interface
│       └── MeditationRepositoryImpl.kt # Room + DataStore integration
├── di
│   ├── DatabaseModule.kt           # Hilt provider for Room
│   └── RepositoryModule.kt         # Hilt binding for Repository
├── service
│   └── SessionTimerService.kt      # Foreground service for timers & audio alerts
└── ui
    ├── navigation
    │   ├── Screen.kt               # Type-safe route definitions
    │   └── NavGraph.kt             # Jetpack Compose Navigation graph
    ├── screens
    │   ├── home                    # Mode selection & stats dashboard
    │   ├── interval                # Interval mode duration & frequency picker
    │   ├── longduration            # Long duration goal & milestone switch
    │   ├── session                 # Active timer, breathing visualizer & modal
    │   ├── history                 # Session history logs & filter chips
    │   └── settings                # Tone, vibration & dynamic theme toggles
    └── theme
        ├── Color.kt                # Calming sage and earth palette
        ├── Theme.kt                # Material You dynamicColor engine
        └── Type.kt                 # Scalable typography system
```

---

## 🚀 15-Minute Quickstart Run Guide

### Requirements
- **Android Studio Iguana | 2023.2.1** or newer (Ladybug / Koala recommended)
- **JDK 17** (configured in Android Studio under *Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK*)
- **Target Android Device / Emulator**: Running Android 8.0 (API 26) or higher (Android 12+ recommended for Material You dynamic color verification).

### Steps to Run
1. Unzip `MeditationTracker.zip`.
2. Open Android Studio and select **Open**, then choose the unzipped `MeditationTracker` folder.
3. Allow Gradle to synchronize dependencies.
4. Select an emulator or physical device.
5. Click **Run** (`Shift + F10`).

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew test
```
The test suite includes:
- `SessionRepositoryTest`: Verifies DAO invocations and flow emissions using MockK.
- `TimeFormatterTest`: Validates minute/second string formatting and edge cases.

---

## 📜 License
MIT License. Free to customize, distribute, and enhance.
