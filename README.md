# OctoBuddy

Free Android pet companion starring the **real Evil0ctopus brand octopus** — a PorkChop-inspired companion shell (speech bubbles, glossy XP, ranks, achievements, level-up fanfare) on a living **cyber-ocean** background.

> **Free forever for core play.** No ads, no Play Billing. A short hook in `PetViewModel` is reserved for future premium cosmetics — not wired up.

## Features (v0.6)

### Brand-true pet + interactive world
- **Hero pet = brand PNG** (`drawable-nodpi/octobuddy_pet.png`) — idle bob / breathe / tilt, cyan eye gleam, stage scale, Adult copper crown, punch & spin on care
- **Interactive cyber-ocean** — navy gradient, soft caustics, hex lattice + circuit nodes; drag for parallax, tap for ripples & bubbles

### PorkChop-style game shell
- **Speech bubbles** with Evil0ctopus cyber-pirate quips (tap / feed / play / rest / ambient / level-up / evolve)
- **Glossy XP bar** + **rank titles** by level (Inkling → … → Dread Octopus)
- **Avatar stages** Hatchling → Juvenile → Adult (scale + Adult accents)
- **Level-up fanfare** and **evolve celebration** (overlay + haptic + quip)
- **Achievements** light layer (first care actions, tap/feed/play milestones, evolve & level goals)
- **Persistent profile** — name, XP, needs, haptics, achievement mask, care counts (DataStore)

### Care loop
- Hunger · Mood · Energy (0–100) with Feed / Play / Rest / tap; decay while away or on a 30s tick
- XP: Tap +1 · Feed +5 · Play +8 · Rest +4 · `level = 1 + floor(xp / 40)` capped at 30
- First-run welcome · rename · Settings (haptics + reset with confirm)
- Always-dark Material 3 theme from brand **navy / cyan / copper**

## Requirements

- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer)
- JDK 17 · device/emulator **API 26+** (target/compileSdk 35)

## Open & run

```bash
git clone https://github.com/Evil0ctopus/octobuddy.git
```

Open in Android Studio → Run ▶ `app`.

Application id: `com.evil0ctopus.octobuddy` · **0.6.0** (versionCode 6)

### Verify (v0.6)

1. Welcome sheet → name buddy; pet is clearly the **Evil0ctopus mark** (not a wrong 3D mesh).
2. Speech bubble appears on tap; ambient quips every ~14s.
3. Drag ocean / tap void for parallax & ripples.
4. Glossy XP bar + rank line update on care; level-up overlay at XP thresholds; evolve at Juvenile/Adult.
5. Trophy icon → achievements list; Settings → haptics / reset.
6. Force-stop → profile persists.

## Design notes

UX/game shell patterns inspired by [PorkChop](https://github.com/0ct0sec/M5PORKCHOP) / [Porkchop-cyd-Port](https://github.com/Xombi3/Porkchop-cyd-Port) and Josh’s [Pocket-Pirate-CYD](https://github.com/Evil0ctopus/Pocket-Pirate-CYD) (speech bubbles, ranks, XP bar, fanfare) — **companion systems only**, not WiFi/attack tooling.

## Art

- Brand mark: [`brand/`](brand/) · in-app `octobuddy_pet.png`
- Optional GLB retained under `assets/models/` for experiments; **v0.6 does not use SceneView**

## License

[MIT](LICENSE) © 2026 Evil0ctopus
