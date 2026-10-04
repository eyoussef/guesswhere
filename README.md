# Guess Where!

**Where in the world was this photo taken?** — a photo geography game built on [Wikimedia Commons](https://commons.wikimedia.org), the free media repository, and OpenStreetMap maps.

Tap the map to drop your pin. The closer you are to where the photo was actually taken, the more points you get. Five rounds, up to 5,000 points each — plus a Daily Mix to keep you coming back.

<p>
  <a href="https://github.com/eyoussef/guesswhere/releases/latest"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/eyoussef/guesswhere?logo=github" /></a>
  <a href="https://github.com/eyoussef/guesswhere/blob/main/LICENSE"><img alt="License: GPL-3.0-only" src="https://img.shields.io/badge/license-GPL--3.0--only-blue" /></a>
  <a href="https://github.com/eyoussef/guesswhere/releases/latest"><img alt="APK asset" src="https://img.shields.io/badge/install-release%20APK-3ddc84" /></a>
</p>

## How it works

1. Pick a **photo pack** — Whole world (random), Coasts and oceans, Mountains, Cities.
2. You get a real photograph from Wikimedia Commons and a world map.
3. Tap anywhere to place your pin, confirm your guess.
4. The reveal shows both markers, the distance, your score — and the photographer credit + license, as free-media attribution should be.
5. After five rounds: total score, personal-best tracking, local leaderboard, share your result.

**Scoring:** a round starts at 5,000 points and decays exponentially with distance, `score = 5000 · e^(−km/λ)`. Full points inside ~2 km. λ depends on difficulty: Easy 2,600 km · Normal 1,400 km · Hard 650 km. Skipping a round scores zero.

**Daily Mix:** the pack and the round ordering are seeded by the date, so every player gets the same challenge on a given day.

## Privacy

No accounts. No analytics. No ads. Scores, settings, and seen-image memory stay on your device, and the OpenStreetMap tile cache is app-private. The app identifies itself to Wikimedia/OSM servers with a User-Agent string; otherwise nothing leaves the device except image and API requests.

Not affiliated with the Wikimedia Foundation.

## Install

**From GitHub Releases** (debug-signed): grab [`GuessWhere-1.0.0.apk`](https://github.com/eyoussef/guesswhere/releases/tag/v1.0.0) — Android 8.0+ (arm64).

**From my F-Droid repository** — signed release APKs served via GitHub Pages:

1. Add repo to the F-Droid client:
   ```
   https://eyoussef.github.io/guesswhere/repo
   ```
2. Fingerprint (SHA-256 of the repo signing key):
   ```
   51 F8 7E 98 45 32 8A BE 84 D2 48 F7 2F 1C 55 07
   A0 2A 41 3B 55 5E BD D8 4B DF AD E6 DD D0 47 88
   ```
3. Install **Guess Where!**.

Pending review for the official F-Droid main repository (metadata MR stage).

## Build it yourself

Requirements: JDK 17+, Android SDK (API 37), Gradle via the wrapper.

```bash
git clone https://github.com/eyoussef/guesswhere.git
cd guesswhere
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:testDebugUnitTest # scoring unit tests
./gradlew :app:assembleRelease   # release APK (needs your keystore.properties)
```

The project keeps no secrets in-tree: release signing reads `keystore.properties` (gitignored — see `app/build.gradle.kts`). Without it, release builds fall back to the debug key.

## Tech stack

Kotlin · Jetpack Compose (Material 3 Expressive) · Coil · Retrofit + kotlinx-serialization · DataStore Preferences · osmdroid (OpenStreetMap) · Paging via MediaWiki `gsroffset` cursor.

Imagery: [Wikimedia Commons](https://commons.wikimedia.org) — each photo shown with its author and license on screen. Maps: © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, rendered by [osmdroid](https://github.com/osmdroid/osmdroid).

## License

[GPL-3.0-only](LICENSE) © 2026 eyoussef1 — this app's *code* is free software. The photos inside are owned by their contributors and carry their own free licenses (shown in-app with every image).