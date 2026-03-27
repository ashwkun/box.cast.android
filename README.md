<div align="center">

# 🎙️ BoxCast

### *The Ultimate Podcast App For Android*

*Built completely with Jetpack Compose featuring a beautiful Material 3 Design.*
*Download the APK below and start listening!*

[![Download APK](https://img.shields.io/badge/⬇_DOWNLOAD_APK-Latest_Release-00C853?style=for-the-badge&logo=android&logoColor=white&labelColor=1a1a2e)](https://github.com/ashwkun/box.cast.android/releases/latest/download/app-release.apk)

[![GPL v3 License](https://img.shields.io/badge/License-GPLv3-ff0080.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Reddit](https://img.shields.io/badge/Connect%20on-Reddit-FF4500.svg?logo=reddit&logoColor=white)](https://www.reddit.com/user/Altruistic_Plenty696)

*Because listening to stories is free and I love podcasts* 🎙️

</div>

---

## 📱 About

BoxCast focuses on clean design, fast discovery, and a playback experience that stays out of your way. Built entirely with Kotlin and Jetpack Compose, it embraces [Material 3 Expressive](https://m3.material.io/) guidelines with spring-based motion, dynamic color extraction, variable typography, and expressive shapes.

---

## ✨ Features

### 🏠 **Home — Your Podcast Hub**
- **Hero Carousel**: Spotlight trending podcasts with full-bleed artwork
- **Curated Time Blocks**: Picks that adapt to your time of day
- **Your Shows**: New episodes from subscriptions
- **Smart Shuffle**: Resume listening instantly

### 🎵 **Player — Beautiful Playback**
- **Dynamic Theming**: Album art colors are extracted in real-time
- **Variable Speed**: 0.5× to 3× with pitch correction
- **Queue & Up Next**: Drag-to-reorder queue
- **Sleep Timer**: Preset durations with a fade-out

### 🔍 **Explore & Search**
- **Genre Browsing**: Filter trending charts by category
- **Hybrid Search**: Local edge database matching + full Podcast Index catalog
- **Region Support**: Browse charts by localized countries

### 📚 **Library & Offline**
- **Offline Downloads**: Save episodes with background downloading
- **Listening History**: Full playback history with resume positions natively synced
- **Liked Episodes**: Quick-access list of hearted content

---

## 📸 Screenshots

<div align="center">

### Feed & Home

<table>
  <tr>
    <td><img src="images/playart_01.jpg" width="200" alt="Discover Podcasts"/><br/><sub><b>Discover</b> - Trending</sub></td>
    <td><img src="images/playart_02.jpg" width="200" alt="Beautiful Player"/><br/><sub><b>Player</b> - Dynamic Theme</sub></td>
    <td><img src="images/playart_03.jpg" width="200" alt="Your Library"/><br/><sub><b>Library</b> - Subscriptions</sub></td>
  </tr>
</table>

### Search & UI Components

<table>
  <tr>
    <td><img src="images/playart_04.jpg" width="200" alt="Smart Search"/><br/><sub><b>Search</b> - Hybrid Results</sub></td>
    <td><img src="images/playart_05.jpg" width="200" alt="Material 3 Expressive"/><br/><sub><b>UI</b> - Material 3</sub></td>
  </tr>
</table>

</div>

---

## 🛠️ Tech Stack

### **Core Technologies**

| Technology | Purpose |
|-----------|---------|
| **Kotlin** | 100% Kotlin codebase for type-safe, concise code |
| **Jetpack Compose** | Modern declarative UI framework with Material 3 Expressive |
| **Clean Architecture** | Predictable state management |
| **Coroutines & Flow** | Asynchronous programming and reactive streams |

### **Networking & Database**

| Component | Usage |
|---------|-------|
| **Retrofit 2** | REST API client |
| **Room** | Local database for user history and library |
| **Cloudflare Workers** | Backend proxy (TypeScript) handling remote queries |
| **Turso (libSQL)** | Distributed edge database storing chart rankings |

### **Media & Playback**

| Component | Description |
|-----------|-------------|
| **ExoPlayer (Media3)** | High performance audio playback for podcasts |
| **MediaSession** | System-level playback controls and background service |
| **Coil** | Image loading with caching and color extraction |

---

## 🌐 Data Sources

This app aggregates data from multiple public sources to provide a unified podcast experience:

| Source | Data Provided |
|--------|---------------|
| **Podcast Index API** | Global catalog of podcasts, episodes, and search results |
| **Apple Podcast Charts** | Daily scraped trending feeds via GitHub Actions |

---

## 🚀 Getting Started

### **Download & Install**

1. Download the latest APK from [Releases](../../releases) or click the **Download APK** badge above
2. Enable "Install from Unknown Sources" in Android settings
3. Install and enjoy! 🎙️

### **Build from Source**

```bash
# Clone the repository
git clone https://github.com/ashwkun/box.cast.android.git
cd box.cast.android

# Build the APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

**Requirements:**
- Android Studio Ladybug or later
- Android SDK 35+
- JDK 17
- Kotlin 1.9+

---

## 🤝 Contributing

This is a personal passion project, but contributions are welcome! Here's how you can help:

1. **Report Bugs** — Open an issue with detailed reproduction steps
2. **Suggest Features** — Share your ideas in the Discussions tab
3. **Submit PRs** — Fork, code, and submit pull requests

---

## 📄 License

This is a personal open-source fan project. All rights reserved.

<div align="center">

### Made with ❤️ and ☕ by a Podcast fan

**If you love podcasts and this app, give it a ⭐ on GitHub!**

[⬆ Back to Top](#%EF%B8%8F-boxcast)

</div>
