# boxcast Roadmap
> Feature roadmap for the boxcast podcast app

---

## 🎯 High-Impact Features

### Offline Downloads
- [ ] Download episodes for offline listening
- [ ] Download manager with progress indicators
- [ ] Auto-download new episodes from subscriptions
- [ ] Storage management & cleanup

### Smart Playlists / Queues
- [ ] Episode queue with drag-to-reorder
- [ ] Custom playlists
- [ ] Auto-queue next episode from same podcast
- [ ] "Play Later" quick action

### Sleep Timer
- [ ] Preset durations (15/30/45/60 min)
- [ ] "End of episode" option
- [ ] Fade out audio before stopping
- [ ] Gentle notification before timer ends

### Variable Playback Speed
- [ ] Speed controls: 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x, 3x
- [ ] Pitch correction (maintain natural voice)
- [ ] Per-podcast speed memory
- [ ] Quick toggle in mini player

### Skip Silence
- [ ] Detect and skip silent portions
- [ ] Adjustable silence threshold
- [ ] Time saved indicator
- [ ] Toggle per episode or globally

---

## 📊 Engagement Features

### Listening Stats
- [ ] Daily/Weekly/Monthly listening time
- [ ] Favorite genres breakdown
- [ ] Listening streak tracking
- [ ] "Year in Podcasts" wrapped-style summary

### Episode Progress Sync
- [ ] Cloud backup of playback positions
- [ ] Multi-device sync
- [ ] Guest account / anonymous sync option

### Share Clips
- [ ] Select 30-60s audio clip with timestamps
- [ ] Generate shareable video/audio snippet
- [ ] Direct share to Instagram Stories, Twitter, etc.
- [ ] Deep link to episode at timestamp

### Episode Notes/Bookmarks
- [ ] Mark timestamps with custom notes
- [ ] Bookmark important moments
- [ ] Export notes as text
- [ ] Quick-access bookmarks list

---

## 🔔 Discovery & Notifications

### New Episode Notifications
- [ ] Push notifications for new episodes
- [ ] Configurable per-podcast
- [ ] Quiet hours setting
- [ ] Batch notification digest option

### Personalized Recommendations (AI-Powered)
- [ ] **Phase 1: Content-Based** - Genre matching from subscriptions
- [ ] **Phase 2: Embedding-Based** - Semantic similarity using vectors
  - Use Cloudflare Workers AI (`@cf/baai/bge-base-en-v1.5`) - FREE
  - Combine: title + description + genre + author + episode titles
  - Generate embeddings once, update incrementally for new podcasts
  - Store in `podcast_similarities` table (pre-computed top 10 similar)
  - Add to existing `sync-pi-data.yml` GitHub Action
- [ ] **Phase 3: Collaborative** (needs 500+ users)
  - "Users who subscribed to X also subscribed to Y"
  - Co-subscription matrix with Jaccard similarity
- [ ] "Recommended For You" carousel on Home
- [ ] "Similar Podcasts" on Podcast Detail page

### Curated Collections
- [ ] Editorial playlists ("Best True Crime 2024")
- [ ] Staff picks
- [ ] Trending by genre
- [ ] Seasonal collections

---

## 🎨 Polish & UX

### Widget Support
- [ ] Home screen widget
- [ ] Now playing info
- [ ] Quick play/pause controls
- [ ] Lock screen controls (already done via MediaSession?)

### Auto-Delete Played Episodes
- [ ] Delete after X days
- [ ] Delete when storage low
- [ ] Keep starred/bookmarked episodes
- [ ] Manual bulk cleanup

### Chapter Support
- [ ] Parse embedded chapter markers
- [ ] Chapter navigation UI
- [ ] Skip to chapter
- [ ] Chapter timestamps in episode info

---

## 🐛 Bug Fixes & Technical Debt

- [x] R8/Retrofit suspend function crash (fixed)
- [x] Genre tabs not loading - case sensitivity (fixed)
- [x] HTTP support (Cleartext traffic) for BBC/legacy podcasts (fixed)
- [x] Explore skeleton loader scope (fixed)
- [x] **PERFORMANCE**: API Cache + Subquery Optimization + DB Indexes (~180ms response)
- [x] Fix duplicate podcast crash in UI (LazyColumn key collision)
- [x] Fix Trending/Charts sync (missing episodes)
- [ ] Remove debug logging before production
- [ ] Add comprehensive error handling
- [ ] Unit tests for repository layer
- [ ] UI tests for critical flows

---

## 🏪 Play Store Readiness

### Privacy & Compliance (Required for Publishing)
- [x] Consent dialog on first launch
- [x] Opt-in by default for analytics
- [x] Analytics blocked without consent
- [x] Firebase collection synced with consent
- [x] **Privacy Policy URL** - Link added in app (`https://aswin.cx/boxcast/privacy`)
  - What data collected (analytics events, crash reports)
  - Data shared with Google (Firebase)
  - How to opt out (in-app toggles)
  - How to request deletion
- [x] **Reset Analytics Data button** in Settings
  - Call `FirebaseAnalytics.resetAnalyticsData()`
  - Clears local cache, generates new anonymous ID
- [ ] **Data Safety Form** - Complete in Play Console
  - App interactions: Yes (with consent)
  - Crash logs: Yes (with consent)
  - Can users request deletion: Yes

### Store Listing
- [ ] App icon (512x512)
- [ ] Feature graphic (1024x500)
- [ ] Screenshots (phone, tablet)
- [ ] Short & full description
- [ ] Content rating questionnaire
- [ ] Target audience declaration

---

## 📅 Priority Order (Suggested)

| Priority | Feature | Effort | Impact |
|----------|---------|--------|--------|
| P0 | Sleep Timer | Small | High |
| P0 | Variable Playback Speed | Small | High |
| P1 | Offline Downloads | Large | Very High |
| P1 | Episode Queue | Medium | High |
| P1 | New Episode Notifications | Medium | High |
| P2 | Skip Silence | Medium | Medium |
| P2 | Listening Stats | Medium | Medium |
| P2 | Widget Support | Medium | Medium |
| P3 | Share Clips | Large | Medium |
| P3 | Chapter Support | Medium | Low |
| P3 | Personalized Recommendations | Large | High |

---

*Last updated: February 2026*
