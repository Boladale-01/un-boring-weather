# Un Boring Weather (Android)

A tactile, real-time 3D weather app for Android built with **Kotlin**, **Jetpack Compose**, and **OpenGL ES 3.0**.

## Features
- **Real-Time 3D Weather Engine (OpenGL ES 3.0):**
  - Procedural 3D cloud clusters driven by live cloud cover % (tap clouds to regenerate with spring physics)
  - Physics-driven 3D rain & snow tilted by live wind speed
  - Dynamic sun & astronomical 3D moon phase lighting
  - Jagged 3D lightning bolts & ambient flashes during thunderstorms
  - Touch-drag 3D camera parallax
- **3D Extruded Temperature & Blueprint Callouts:**
  - Giant multi-layer extruded temperature display
  - Tap the scene to toggle technical callouts (`CLOUDS`, `WIND`, `TEMP`, `FEELS`, `HUM`, `DEW`, `INHG`, `UV`, `VIS`, `RISE/SET`, `MOON`)
- **Play the Weather Timeline & 8-Day Forecast:**
  - Scrub horizontally across 24 hours with a floating time bubble, rain-wave fill, and tactile haptic/audio detents
  - Toggle to an 8-day forecast strip
- **Swipeable Detail Panels:**
  - **Precip:** 3D terrain slab, rate/chance/type telemetry, and scrubbable minute/hourly chart
  - **Moon:** Phase disc, illumination %, sunrise/sunset, next full/new moon
  - **AQI & Pollen:** US AQI rating, PM2.5, PM10, pollen, UV & visibility
  - **Cloudburst Minigame:** Built-in storm-cloud popping game
- **Collectible 3D Skins & Pro Unlock:**
  - Free: `Graphite`, `Chroma`, `Paper`
  - Pro: `Guava`, `Karat` (metallic gold shader), `Monsters` (googly eyes), `Normal` (RGB surface-normal debug shader), `Wireframe` (3D wireframe shader)
  - Built-in **Gumroad License Key** verification (`UNBORING-PRO-DEMO` works for local testing)
- **In-App Auto-Updates:**
  - Checks **GitHub Releases** automatically and installs new `.apk` releases in-app via `FileProvider`.

## Setup for Your Own Release
1. In `app/src/main/kotlin/com/unboring/weather/data/AppUpdater.kt`, set `GITHUB_OWNER` and `GITHUB_REPO` to your public GitHub repository.
2. In `app/src/main/kotlin/com/unboring/weather/data/LicenseManager.kt`, set `CHECKOUT_URL` and `GUMROAD_PRODUCT_ID` to your Gumroad product.
3. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Create a GitHub Release (e.g. `v1.1`) and attach the `.apk`—existing users will see an in-app **UPDATE** button automatically.
