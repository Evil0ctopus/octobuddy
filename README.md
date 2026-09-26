# OctoBuddy

Free Android pet companion starring the **real Evil0ctopus brand octopus** — a PorkChop-inspired companion shell (speech bubbles, glossy XP, ranks, achievements, **daily challenges**, **care streaks**, **cosmetics**) on a living **cyber-ocean** background.

> **Free forever for core play.** No ads, no Play Billing. A short hook in `PetViewModel` is reserved for future premium cosmetics — not wired up.

## Features (v0.8)

### Brand-true pet + interactive world
- **Hero pet = Blender-animated brand PNG** — exact Evil0ctopus mark warped in Blender (textured plane + armature/lattice); PNG frame loops in `assets/pet_anim/`
- **Distinct care clips** — Idle (32) · Feed (16) · Play (20) · Rest (18) · Tap (12) @ 24fps; mood tints / speed-modulates idle; cosmetics stay as overlays
- **Interactive cyber-ocean** — day/night tint from local clock, mood-tinted water, soft caustics, hex lattice + circuit nodes; drag for parallax, tap for ripples; **particle bursts on care**

### Daily challenges + care streak
- **1–3 daily goals** (feed / play / rest / taps / XP / keep mood or hunger) — stable set per local day key
- **Streak counter** for consecutive days with any care (persisted in DataStore)
- Calendar sheet + HUD chip (🔥 streak · Daily x/y)

### Unlockables / cosmetics
- Equip **frames**, **glitter / tentacle glow**, **mini hats & crowns**, **background accents**
- Unlocked via achievements, level, or streak — manage in the Unlocks sheet; drawn on `BrandPetView`

### PorkChop-style game shell
- Mood-weighted **speech bubbles** (low hunger/energy/mood pick relevant quips)
- Glossy XP bar + rank titles (Inkling → … → Dread Octopus)
- Avatar stages Hatchling → Juvenile → Adult
- Level-up / evolve fanfare + expanded **achievements** (streaks, daily sweep, deeper care milestones)

### Care loop
- Hunger · Mood · Energy (0–100) with Feed / Play / Rest / tap; decay while away or on a 30s tick
- XP: Tap +1 · Feed +5 · Play +8 · Rest +4 · `level = 1 + floor(xp / 40)` capped at 30
- First-run welcome · rename · Settings (haptics + reset with confirm)
- Always-dark Material 3 theme from brand **navy / cyan / copper**

## Install on your phone (one tap)

Sideload the release APK — no Android Studio required.

1. On your **Android phone**, open:  
   **[Download OctoBuddy 0.8.1 APK](https://github.com/Evil0ctopus/octobuddy/releases/download/v0.8.1/octobuddy-0.8.1-release.apk)**  
   (or the [release page](https://github.com/Evil0ctopus/octobuddy/releases/tag/v0.8.1))
2. Tap **Download**, then open the file when prompted.
3. If Android asks, allow **Install unknown apps** for your browser/Files, then tap **Install**.

Requires Android 8.0+ (API 26). The APK is **release-signed** for sideload (not Play Store). Play upload later needs the same upload keystore kept privately on the build machine.

## Requirements

- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer)
- JDK 17 · device/emulator **API 26+** (target/compileSdk 35)

## Open & run

```bash
git clone https://github.com/Evil0ctopus/octobuddy.git
```

Open in Android Studio → Run ▶ `app`.

Application id: `com.evil0ctopus.octobuddy` · **0.8.1** (versionCode 10)

### Verify (v0.8)

1. Welcome sheet → name buddy; pet is the **Evil0ctopus mark**.
2. Calendar icon → daily challenges; care advances progress; streak chip updates.
3. Sparkle icon → Unlocks; earn an achievement / streak and equip a frame, glow, or hat.
4. Feed / Play / Rest show distinct motion; ocean bursts particles; night stars after dusk.
5. Low needs → mood-weighted ambient quips; trophy sheet lists expanded achievements.
6. Force-stop → profile, dailies, streak, and equipped cosmetics persist.

## Design notes

UX/game shell patterns inspired by [PorkChop](https://github.com/0ct0sec/M5PORKCHOP) / [Porkchop-cyd-Port](https://github.com/Xombi3/Porkchop-cyd-Port) and [Pocket-Pirate-CYD](https://github.com/Evil0ctopus/Pocket-Pirate-CYD) (speech bubbles, ranks, XP bar, fanfare, challenge energy) — **companion systems only**, not WiFi/attack tooling.

## Art

- Brand mark: [`brand/`](brand/) · in-app `octobuddy_pet.png`
- **Pet animation** authored in **Blender** from that brand PNG (not a different octopus mesh). Pipeline: [`tools/brand_pet_anim/`](tools/brand_pet_anim/) → `app/src/main/assets/pet_anim/{idle,feed,play,rest,tap}/frame_XX.png`
- Optional GLB retained under `assets/models/` for experiments; **v0.8 still does not use SceneView**

## License

[MIT](LICENSE) © 2026 Evil0ctopus
