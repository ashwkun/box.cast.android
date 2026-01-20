# Box Cast Project Guidelines

**Reference App:** PixelPlayer at `/Users/aswinc/PixelPlayer-reference`
**Current Status:** Fully adopting **Material 3 Expressive (2025 Updates)**.

This document serves as the **Constitution** for all development on the Box Cast Android application. Every line of code and every UI component must adhere to these rules.

## 1. Material 3 Expressive Design Rules (STRICT)

**Core Philosophy:**
1.  **Flexibility:** Components adapt to content (e.g., text wraps, lists segment).
2.  **Playfulness:** Motion is bouncy and spring-based. Shapes are varied (Polygons, Stars).
3.  **Boldness:** Type is variable and expressive. Layouts are distinct (Staggered/Mosaic).

### A. Motion & Animation (Expressive Physics)
*   **Verification Question:** *Does this component feel alive? If it just appears/disappears without bounce, it is rejected.*
*   **Physics Over Easing:**
    *   **Springs:** Use `ExpressiveMotion.BouncySpring` (damping: 0.45f) for organic releases/entrances.
    *   **Interact:** Use `ExpressiveMotion.QuickSpring` for touch responses.
    *   **BANNED:** Linear tweens for UI interactions.
*   **Tactile Feedback:**
    *   **MUST USE:** `Modifier.expressiveClickable { }` for all interactive surfaces.
    *   **Behavior:** Scale down to 0.85f on press, bounce back to 1.0f on release.
*   **Loaders:**
    *   Use **Wavy** indicators (`BoxCastLoader.CircularWavy`) instead of standard circular.
    *   Use **Morphing Shapes** (`BoxCastLoader.Expressive`) for hero loading.

### B. Typography (Variable & Flexible)
*   **Font Family:** **Roboto Flex** (Variable Font).
*   **Implementation:** Use `TopControlBar` pattern for variable axes (`wght`, `wdth`, `slnt`).
*   **Scale:**
    *   **Display:** Large, expressive, often thinner (300-400) or ultra-black (900).
    *   **Titles:** Medium weight (500) for hierarchy.
    *   **Labels:** Medium (500) tracking loose.
*   **Flexible Layouts:**
    *   **App Bars:** Must adjust height based on content (`Medium/Large Flexible`).
    *   **Text Wrapping:** Titles in standard lists/cards should wrap gracefully (2-3 lines), never truncate prematurely.

### C. Shapes & Form (ExpressiveShapes)
*   **Library:** STRICTLY use `ExpressiveShapes` (not `RoundedCornerShape` only).
*   **Vocabulary:**
    *   **Standard:** `ExtraLarge` (28dp) for Cards/Dialogs.
    *   **Expressive:** `Sunny` (8-point star), `Burst` (12-point), `Puffy` (Cloud-like), `Cookie` (N-gon).
*   **Usage:**
    *   **Backgrounds:** Random floating shapes (`ExpressiveBackground` in `ResumeGridCard`).
    *   **Avatars:** Morphing shapes or `Pill`/`Squircle`.
    *   **Cards:** High rounding (`MaterialTheme.shapes.extraLarge`).

### D. Layouts (Staggered & Mosaic)
*   **Grids:** Avoid rigid uniform grids.
    *   **Use:** `StaggeredVerticalGrid` or custom logic like `ResumeGridCard` (Bento/Mosaic).
    *   **Logic:** 1 item = Full width. 2 items = Split vertically. 3 items = 1 Large + 2 Stacked.
*   **Lists:**
    *   **Segmented:** List items are distinct blocks with individual backgrounds (`surfaceContainer`), not continuous.
    *   **Spacing:** Increased padding (16dp+) between segments.

## 2. Component Guidelines (BoxCast Specific)

### A. Navigation & Structure
*   **Top Bar:**
    *   **Standard:** Custom `TopControlBar`.
    *   **Behavior:** Scroll-driven animation (Expanded -> Collapsed).
    *   **Transition:** `Surface` -> `SurfaceContainerLow`.
*   **Cards:**
    *   **Hero:** `HeroCard` with `HorizontalMultiBrowseCarousel`.
    *   **Grid:** `ResumeGridCard` with dynamic `GridLayout` logic.
*   **Loading:**
    *   **Centralized:** Always use `BoxCastLoader`.
        *   `BoxCastLoader.Expressive()`: Hero/Fullscreen.
        *   `BoxCastLoader.CircularWavy()`: Indeterminate.

### B. Color (Dynamic & Role-Based)
*   **System:** Material You Dynamic Colors.
*   **Surface Roles:**
    *   `Surface`: App background.
    *   `SurfaceContainerLow`: Collapsed headers/nav.
    *   `SurfaceContainer`: List items / Cards.
    *   `SurfaceContainerHigh`: Modals / Emphasis.
*   **Extraction:** Media players MUST extract colors from Album Art.

## 3. Architecture Protocol (Feature-First)

**Structure:**
-   `feature/home/`: Components (`HeroCard`), Screens (`HomeScreen`), Logic (`HomeViewModel`).
-   `core/designsystem/`: The System (`ExpressiveShapes`, `ExpressiveMotion`, `Theme`).
-   `core/model/`: Shared data models (`Podcast`, `HeroItem`).

**Testing & Verification:**
1.  **Visual:** does it match `m3.material.io` Expressive?
2.  **Motion:** Does it bounce? (Use `expressiveClickable`).
3.  **Flexibility:** Does it handle long text?

---
*Compliance with this document is mandatory for all code changes.*
