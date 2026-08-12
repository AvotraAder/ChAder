# ChAder 💬

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg?style=flat&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

ChAder is a high-performance, real-time messaging application for Android, built with a **Local-First** architecture. It delivers a fluid, expressive user experience using **Jetpack Compose** and **Material Design 3**, designed to adapt seamlessly across mobile, foldables, and tablets.

## ✨ Key Features

*   **⚡ Real-Time Sync:** Instant messaging powered by **Firebase Firestore** with multi-device synchronization.
*   **📦 Local-First Architecture:** Full offline support via **Room Database**. Chat anytime; data syncs automatically when online.
*   **📱 Adaptive UI:** Intelligent layouts using `ListDetailPaneScaffold` that optimize for large screens and foldables.
*   **🔐 Seamless Auth:** One-tap entry via **Google Sign-In** or Email, removing friction from the onboarding process.
*   **📸 Stories:** Ephemeral visual updates with a vibrant, gesture-driven interface.
*   **🎨 Expressive Design:** Modern aesthetics with edge-to-edge support, dynamic colors, and smooth spring-based animations.

## 🛠 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) |
| **Local DB** | [Room](https://developer.android.com/training/data-storage/room) |
| **Backend** | [Firebase Firestore](https://firebase.google.com/docs/firestore) |
| **Navigation** | Jetpack Navigation 3 |
| **Async** | Kotlin Coroutines & Flow |
| **Storage** | DataStore Preferences |
| **Images** | [Coil](https://coil-kt.github.io/coil/) |

## 🏗 Architecture & Implementation

### Data Consistency & Conflict Resolution
ChAder uses a unified data model designed for dual-engine compatibility (Local Room + Cloud Firebase). 
To resolve JVM signature conflicts between database fields and helper properties, we implement specific annotation targeting:

```kotlin
@get:Ignore  // For Room
@get:Exclude // For Firebase
val isEdited: Boolean get() = edited
```

### Performance Optimization
- **Reactive Streams:** End-to-end implementation of `Flow` from Database/Firestore to the UI layer.
- **Efficient Rendering:** Uses Compose best practices to minimize recompositions in high-frequency chat environments.
- **Adaptive Layouts:** Implements the latest `androidx.compose.material3.adaptive` APIs for future-proof responsiveness.

## 🚀 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/ChAder.git
    ```
2.  **Add Firebase:**
    - Create a project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app and download the `google-services.json`.
    - Place it in the `app/` directory.
3.  **Build & Run:**
    - Open the project in Android Studio (Ladybug or newer).
    - Sync Gradle and run on an emulator or physical device.

---
*Developed with ❤ focusing on modern Android standards.*
