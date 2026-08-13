# Hush 🤫

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg?style=flat&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Hush is a high-performance, real-time messaging application for Android, built with a **Privacy-First** philosophy. It delivers a fluid, expressive user experience using **Jetpack Compose** and **Material Design 3**, designed to adapt seamlessly across mobile, foldables, and tablets.

## 🔒 Encryption & Security (Core Feature)

Security isn't just an option in Hush; it's the foundation. The app implements a **Symmetric Encryption Layer** to ensure that your conversations remain private.

*   **Custom Encryption Keys:** Users can set unique encryption keys for each individual chat. 
*   **XOR Symmetric Chiffre:** Messages are encrypted using a fast and efficient XOR algorithm before they ever leave your device.
*   **Privacy-Centric Sync:** Even though data is synchronized via Firebase, the content of your messages remains unreadable to anyone who doesn't possess your specific chat key.
*   **Zero-Trace Local Cache:** Local data stored in the Room database is kept as plain text for performance but protected by the Android System's secure sandbox.

> **Note:** For maximum security, always share your chat encryption keys via a separate, secure channel.

## ✨ Key Features

*   **⚡ Real-Time Sync:** Instant messaging powered by **Firebase Firestore** with multi-device synchronization.
*   **📦 Local-First Architecture:** Full offline support via **Room Database**. Chat anytime; data syncs automatically when online.
*   **📱 Adaptive UI:** Intelligent layouts using `ListDetailPaneScaffold` that optimize for large screens and foldables.
*   **🔐 Seamless Auth:** One-tap entry via **Google Sign-In** with JWT verification or Email.
*   **🎨 Expressive Design:** Modern aesthetics with edge-to-edge support, dynamic colors (Dark/Light mode), and smooth animations.

## 🛠 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) |
| **Local DB** | [Room](https://developer.android.com/training/data-storage/room) |
| **Backend** | [Firebase Firestore](https://firebase.google.com/docs/firestore) |
| **Storage** | Firebase Storage (Encrypted pathing) |
| **Navigation** | Jetpack Navigation 3 |
| **Async** | Kotlin Coroutines & Flow |
| **Security** | Custom Symmetric XOR Layer + JWT Token Decoding |

## 🏗 Architecture & Implementation

### Data Consistency
Hush uses a unified data model designed for dual-engine compatibility. We implement specific repository patterns to handle the bridge between local Room persistence and remote Firestore updates, ensuring that message statuses (SENT, RECEIVED, SEEN) are always accurate.

### Performance Optimization
- **Reactive Streams:** End-to-end implementation of `Flow` from Database/Firestore to the UI layer.
- **Efficient Rendering:** Minimizes recompositions using `immutable` state patterns.
- **Adaptive Layouts:** Implements the latest `androidx.compose.material3.adaptive` APIs.

## 🚀 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/Hush.git
    ```
2.  **Add Firebase:**
    - Create a project in the [Firebase Console](https://console.firebase.google.com/).
    - Add an Android app and download the `google-services.json`.
    - Place it in the `app/` directory.
    - Enable **Firestore**, **Auth (Google/Email)**, and **Storage**.
3.  **Build & Run:**
    - Open the project in Android Studio.
    - Sync Gradle and run on your device.

---
*Hush - Silence the noise, secure the message.*
