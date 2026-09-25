# OctoBuddy

Free Android pet companion featuring the **Evil0ctopus** brand octopus — now with **XP, evolution stages, and an animated 3D pet**.

Tap your buddy to boost mood, feed when hungry, play to cheer them up, rest when tired, and watch a **SceneView (Filament) 3D octopus** idle-bob while Hunger, Mood, and Energy drift over time (local-only, DataStore). Care actions earn XP so your hatchling grows into a juvenile, then an adult. Rename your pet so status copy feels personal.

> **Free now · premium later.** The app ships without ads or Play Billing. A short hook in code (`PetViewModel`) is reserved for future premium cosmetics / boosts — not implemented in this scaffold.

## Features (v0.4)

- **Animated 3D pet** via SceneView / Filament (bundled `octobuddy.glb`); PNG fallback if load fails
- **XP + levels + evolution stages**
  - Tap +1 XP · Feed +5 · Play +8 · Rest +4
  - `level = 1 + floor(xp / 40)`, capped at **30**
  - **Hatchling** levels 1–4 · **Juvenile** 5–9 · **Adult** 10+
  - Stage changes visible scale + idle speed on the 3D model
- Hunger + Mood + Energy (0–100), decay while away or on a 30s tick
- Feed / Play / Rest care actions with a short 3D punch / spin
- Rename your pet (persisted via DataStore); status copy uses the name + stage when happy
- Light haptic on tap; stronger haptic on Feed / Play / Rest
- Material 3 · Kotlin · Jetpack Compose · minSdk 26

## Requirements

- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer recommended)
- JDK 17
- Android device or emulator with **API 26+** (minSdk 26; target/compileSdk 35)
- OpenGL ES 3.0 (required for SceneView / Filament)

## Open & run

1. Clone this repo:
   ```bash
   git clone https://github.com/Evil0ctopus/octobuddy.git
   ```
2. Open the **octobuddy** folder in Android Studio (**File → Open**).
3. Let Gradle sync (Android Studio downloads the wrapper/deps on first open).
4. Pick an emulator or USB device → **Run ▶** `app`.

Application id: `com.evil0ctopus.octobuddy` · versionName **0.4.0** (versionCode 4)

### Verify on device (v0.4 checklist)

1. Fresh install → pet should appear as a **small (Hatchling) animated 3D octopus** (gentle bob + spin), not a flat static PNG.
2. Status row shows **Level 1 · Hatchling** and an XP progress bar.
3. Tap the pet a few times → mood + XP tick up; 3D model does a short punch/spin.
4. Feed / Play / Rest → needs update, XP gains (+5 / +8 / +4), stronger haptic, action burst on the model.
5. Keep caring until **160 XP** (level 5) → stage becomes **Juvenile** and the model scales up; at **360 XP** (level 10) → **Adult**.
6. Force-stop and reopen → XP / level / stage / name persist.

## Art attribution

- **2D brand art:** Evil0ctopus mark under [`brand/`](brand/) (spare under [`assets/`](assets/)). In-app fallback drawable: `app/src/main/res/drawable-nodpi/octobuddy_pet.png`.
- **3D pet:** original stylized GLB (CC0) — see [`app/src/main/assets/models/README.md`](app/src/main/assets/models/README.md).

## License

[MIT](LICENSE) © 2026 Evil0ctopus
