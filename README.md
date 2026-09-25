# OctoBuddy

Free Android pet companion featuring **Evil0ctopus** (Evil0ctopus) brand octopus.

Tap your buddy to boost mood, feed when hungry, play to cheer them up, and watch a gentle idle bob while Hunger and Mood drift over time (local-only, DataStore). Rename your pet so status copy feels personal.

> **Free now · premium later.** The app ships without ads or Play Billing. A short hook in code (`PetViewModel`) is reserved for future premium cosmetics / boosts — not implemented in this scaffold.

## Features (v0.2)

- Home / pet screen with brand octopus art
- Hunger + Mood (0–100), decay while away or on a 30s tick
- Tap pet → mood boost · **Feed** → hunger (and a little mood) · **Play** → mood up, slight hunger cost
- Rename your pet (persisted via DataStore); status copy uses the name
- Light haptic on tap; stronger haptic on Feed / Play
- Friendly status copy (“{name} is happy / hungry / sleepy …”)
- Compose idle motion (bob + soft scale pulse)
- Material 3 · Kotlin · Jetpack Compose

## Requirements

- [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer recommended)
- JDK 17
- Android device or emulator with **API 26+** (minSdk 26; target/compileSdk 35)

## Open & run

1. Clone this repo:
   ```bash
   git clone https://github.com/Evil0ctopus/octobuddy.git
   ```
2. Open the **octobuddy** folder in Android Studio (**File → Open**).
3. Let Gradle sync (Android Studio downloads the wrapper/deps on first open).
4. Pick an emulator or USB device → **Run ▶** `app`.

Application id: `com.evil0ctopus.octobuddy`

## Art attribution

Pet art is Evil0ctopus brand octopus. Source files live under [`brand/`](brand/) (and a spare under [`assets/`](assets/)). The in-app drawable is `app/src/main/res/drawable-nodpi/octobuddy_pet.png`.

## License

[MIT](LICENSE) © 2026 Evil0ctopus
