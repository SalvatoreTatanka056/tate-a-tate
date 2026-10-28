# Tête-à-Tête

**Tête-à-Tête** is a private peer-to-peer (P2P) chat Android application that leverages Google Drive as a decentralized backend storage for secure and direct messaging between users.

## Features

- **P2P Messaging via Google Drive**: Connect with a peer using unique room IDs and synchronize chat messages securely through Google Drive files.
- **Modern Material Design 3 UI**: Clean, intuitive chat interface supporting dark/light styling, connection status indicators, offline detection, and empty states.
- **Network Awareness**: Real-time connectivity monitoring using `ConnectivityManager` with automatic UI updates and offline banners.
- **Clipboard Integration**: Quick copy and paste of room IDs for seamless connection sharing.

## Tech Stack

- **Language**: Java
- **UI**: Android Views, Material Components (`ConstraintLayout`, `RecyclerView`, `FloatingActionButton`, `CardView`)
- **Backend / Sync**: Google Sign-In, Google Drive API v3, Google HTTP Client Gson
- **Lifecycle & Architecture**: AndroidX libraries, Activity Result API, Network Callbacks

## Getting Started

1. Open the project in Android Studio (Arctic Fox / Iguana or newer).
2. Sync the project with Gradle files.
3. Configure your Google Sign-In / OAuth credentials for Google Drive API access if running on a physical or virtual device.
4. Build and run the app (`app:assembleDebug`).
