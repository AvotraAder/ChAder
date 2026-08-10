# ChAder

ChAder is a streamlined, responsive real-time chat application built with a focus on speed and modern aesthetics. Utilizing Material Design 3, the app provides a vibrant, energetic user experience that seamlessly adapts across various Android device form factors, from mobile phones to foldables and tablets.

## Features

*   **Simplified Authentication:** Quick and secure access via Email or Google Sign-In, eliminating the need for a traditional registration flow and streamlining the onboarding process.
*   **Real-Time Messaging:** Instantaneous text communication with a reactive UI that updates as messages arrive, powered by Room and Kotlin Flow.
*   **Stories:** A dedicated, visually-driven section for sharing ephemeral updates, featuring a vibrant gradient UI.
*   **Adaptive Communication Hub:** A dynamic interface using multi-pane layouts (`ListDetailPaneScaffold`) to provide an optimized viewing experience on both small and large screens.
*   **Edge-to-Edge Experience:** Full support for modern Android display standards, utilizing the entire screen area while respecting system bars.

## Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose with Material Design 3 (energetic color scheme)
*   **Navigation:** Jetpack Navigation 3 (State-driven navigation logic)
*   **Adaptive Strategy:** Compose Material Adaptive library for responsive, multi-pane layouts.
*   **Database:** Room (for local persistence and real-time data streams)
*   **Networking:** Retrofit and OkHttp for API communication.
*   **Image Loading:** Coil for efficient rendering of user avatars and stories.
*   **Session Management:** DataStore Preferences for secure user session persistence.

## Simplified Authentication

ChAder prioritizes user convenience by offering a "Single Entry" authentication flow. Instead of a multi-step signup process, users can authenticate directly using their Email or Google account. This approach reduces friction and ensures users can start chatting immediately.

## Design Philosophy

The app follows **Material Design 3 Expressive** guidelines, characterized by:
- **Vibrant Colors:** A high-chroma palette that shifts between light and dark modes.
- **Modern Iconography:** Adaptive icons and rounded Material Symbols.
- **Expressive Motion:** Smooth, spring-based animations for transitions and interactions.
