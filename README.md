# boxcast

A podcast app for Android, built with Kotlin and Jetpack Compose.

Boxcast focuses on clean design, fast discovery, and a playback experience that stays out of your way. It follows [Material 3 Expressive](https://m3.material.io/) guidelines — spring-based motion, dynamic color extraction, variable typography, and expressive shapes throughout.

---

## Features

### Home
- **Hero Carousel** — Spotlight trending podcasts with full-bleed artwork and one-tap playback.
- **Curated Time Blocks** — Morning, afternoon, and evening picks that adapt to when you open the app.
- **Your Shows** — New episodes from subscriptions, synced live via the Podcast Index API.
- **Smart Shuffle** — Resume listening with a mosaic grid of recently played and new episodes from your library.

### Player
- **Dynamic Theming** — Album art colors are extracted in real-time and applied to the entire player surface.
- **Sleep Timer** — Preset durations with a fade-out before stopping.
- **Variable Speed** — 0.5× to 3× with pitch correction.
- **Queue & Up Next** — Drag-to-reorder queue with mark-as-played and play-next actions.
- **Seek Controls** — 10-second skip forward/backward with spring-animated feedback.

### Explore
- **Genre Browsing** — Filter trending charts by category (News, Comedy, True Crime, etc.).
- **Grid Layout** — Staggered tile layout for visual variety over rigid uniform grids.
- **Region Support** — Browse charts by country.

### Library
- **Subscriptions** — All your podcasts in one place.
- **Downloads** — Save episodes for offline listening with a background download service.
- **Liked Episodes** — Quick-access list of episodes you've hearted.
- **Listening History** — Full playback history with resume positions.

### Search
- **Hybrid Search** — Queries hit both the local edge database (for instant chart matches) and the Podcast Index API (for global coverage), then merges results.
- **Subscription Badges** — Results are cross-referenced with your local Room database to badge podcasts you already follow.

### Other
- **Onboarding** — Guided genre picker on first launch to personalize the Home feed.
- **Privacy Consent** — Opt-in analytics with a clear consent dialog. Firebase Analytics and Crashlytics are blocked until the user explicitly agrees. A "Reset Analytics Data" button is available in Settings.
- **In-App Updates** — Version check against GitHub Releases with a migration dialog for signing key changes.

---

## Design

Boxcast adopts Material 3 Expressive as its design language:

- **Motion** — Spring physics (`BouncySpring`, `QuickSpring`) instead of linear easing. All interactive surfaces scale down on press and bounce back on release via `expressiveClickable`.
- **Typography** — Roboto Flex variable font with configurable weight, width, and slant axes.
- **Shapes** — Uses `ExpressiveShapes` (Sunny, Burst, Puffy, Cookie) for backgrounds and avatars alongside standard high-radius rounding for cards and dialogs.
- **Color** — Material You dynamic colors with a structured surface role hierarchy (`Surface` → `SurfaceContainerLow` → `SurfaceContainer` → `SurfaceContainerHigh`).
- **Layouts** — Staggered and mosaic grids that adapt item sizing based on count (1 item = full width, 2 = split, 3 = 1 large + 2 stacked).
- **Loading** — Custom wavy circular indicators and morphing-shape loaders instead of standard spinners.

---

## Architecture

### Android App

Multi-module Gradle project following a feature-first architecture:

```
app/                  → Main entry point, navigation, DI
core/
  ├── network/        → Retrofit API client, network models
  ├── data/           → Repositories, Room database, DAOs, playback service, download service
  ├── model/          → Shared domain models (Podcast, Episode)
  └── designsystem/   → Theme, typography, shapes, motion, reusable components
feature/
  ├── home/           → Home screen, hero carousel, curated sections, smart shuffle
  ├── player/         → Full player, queue, speed/sleep controls
  ├── info/           → Podcast detail, episode detail
  ├── explore/        → Genre browsing, charts
  ├── library/        → Subscriptions, downloads, liked, history
  └── onboarding/     → First-launch genre picker
```

**Key dependencies:** Jetpack Compose, Material 3, Coil (image loading), Retrofit + kotlinx.serialization (networking), Room (local DB), Media3/ExoPlayer (playback), Firebase Analytics & Crashlytics.

### Backend (Proxy)

A **Cloudflare Worker** (TypeScript) that sits between the Android app and the [Podcast Index API](https://podcastindex.org/):

- **Edge Database** — [Turso](https://turso.tech/) (distributed SQLite) stores chart rankings, podcast metadata, and pre-synced episodes. Discovery endpoints like `/trending` are served directly from the database for low-latency responses.
- **Live Proxy** — Subscription sync (`/sync`) and individual podcast/episode lookups are proxied to the Podcast Index API in parallel for guaranteed freshness.
- **Hybrid Search** — `/search` merges results from the edge database (chart matches) and Podcast Index (global catalog).
- **Data Pipeline** — A GitHub Actions workflow runs daily to scrape Apple Podcast charts, import the Podcast Index data dump, and sync episodes for top-charting podcasts.

---

## Project Setup

### Prerequisites

- Android Studio Ladybug or later
- JDK 17
- Android SDK 35

### Configuration

Create a `local.properties` file in the project root:

```properties
sdk.dir=/path/to/android/sdk
BOXCAST_API_BASE_URL=https://your-worker.your-domain.workers.dev
BOXCAST_PUBLIC_KEY=your_podcast_index_public_key
KEY_STORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```

### Build & Run

```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Signed release build
```

Or run directly from Android Studio.

### Proxy (Cloudflare Worker)

```bash
cd proxy
npm install
npm run dev       # Local development
npm run deploy    # Deploy to Cloudflare
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 Expressive |
| Image Loading | Coil |
| Networking | Retrofit + kotlinx.serialization |
| Local Database | Room |
| Playback | Media3 / ExoPlayer |
| Analytics | Firebase Analytics + Crashlytics |
| Backend | Cloudflare Workers (TypeScript) |
| Edge Database | Turso (libSQL) |
| Data Source | Podcast Index API |
| CI/CD | GitHub Actions |

---

## License

This is a personal project. All rights reserved.
