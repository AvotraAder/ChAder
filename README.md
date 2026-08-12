# ChAder

ChAder is a streamlined, responsive real-time chat application built with a focus on speed and modern aesthetics. Utilizing Material Design 3, the app provides a vibrant, energetic user experience that seamlessly adapts across various Android device form factors, from mobile phones to foldables and tablets.

## Features

*   **Simplified Authentication:** Quick and secure access via Email or Google Sign-In, eliminating the need for a traditional registration flow and streamlining the onboarding process.
*   **Real-Time Messaging:** Instantaneous communication with a reactive UI that updates as messages arrive, powered by Firebase Firestore for cloud sync and Room for local persistence.
*   **Stories:** A dedicated, visually-driven section for sharing ephemeral updates, featuring a vibrant gradient.
*   **Adaptive Communication Hub:** A dynamic interface using multi-pane layouts (`ListDetailPaneScaffold`) to provide an optimized viewing experience on both small and large screens.
*   **Edge-to-Edge Experience:** Full support for modern Android display standards, utilizing the entire screen area while respecting system bars.

## Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose with Material Design 3 (energetic color scheme)
*   **Navigation:** Jetpack Navigation 3 (State-driven navigation logic)
*   **Adaptive Strategy:** Compose Material Adaptive library for responsive, multi-pane layouts.
*   **Database:** Room (for local persistence and real-time data streams)
*   **Cloud Backend:** Firebase Firestore (for real-time synchronization)
*   **Networking:** Retrofit and OkHttp for API communication.
*   **Image Loading:** Coil for efficient rendering of user avatars and stories.
*   **Session Management:** DataStore Preferences for secure user session persistence.

## Project Notes

### Data Model Resolution
The application uses a unified `Message` data model that is compatible with both Room and Firebase Firestore. To handle potential conflicts with Kotlin boolean property naming conventions (e.g., `isEdited` vs `edited`), the model uses specific annotations (`@get:Ignore` and `@get:Exclude`) to ensure smooth serialization and database mapping without ambiguity.

## Design Philosophy

The app follows **Material Design 3 Expressive** guidelines, characterized by:
- **Vibrant Colors:** A high-chroma palette that shifts between light and dark modes.
- **Modern Iconography:** Adaptive icons and rounded Material Symbols.
- **Expressive Motion:** Smooth, spring-based animations for transitions and interactions.
