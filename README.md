# Better Me

A standalone Android companion app for wellbeing — Zen, Mindfulness, Inner Child, Inner Peace, Empowerment.

> V1 — offline-first, bundled AI-generated content packs, Garmin Connect integration for biometric-driven daily tips.

## Modules

| Screen | Purpose |
|---|---|
| **Home** | Personal greeting + daily quote + nature concept art background |
| **Daily Tips** | Garmin-powered, rule-based tips refreshed once a day + midday notification |
| **Meals** | Ingredient picker → day/week meal plan with quantities |
| **Calories** | Food log with OpenFoodFacts search, manual entry, daily target ring |
| **Gym** | Animated exercise library + auto-generated split routines |
| **Evolution** | Weight tracking graph + monthly highlights |

## Tech

- Kotlin · Jetpack Compose · Material 3
- Hilt · Room · DataStore · WorkManager · Retrofit
- Lottie (exercise animations) · Vico (charts)
- Min SDK 26 · Target SDK 35 · JDK 17

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Open in Android Studio Koala (or newer). First-time sync downloads the Gradle wrapper (8.x) and the Android SDK as configured.

## Content packs

Offline content lives in `app/src/main/assets/content/`. Regenerate via:

```bash
cd scripts/generate_packs
py generate_packs.py all
```

See [scripts/generate_packs/README.md](scripts/generate_packs/README.md).

## Garmin integration

See [docs/garmin-setup.md](docs/garmin-setup.md). Developer credentials are issued by Garmin within ~1 week. Until then the app runs against the `GarminClientStub` and the tip engine falls back to default mindfulness messages.

## Design system

| Token | Hex |
|---|---|
| Teal Deep (primary) | `#0F4C5C` |
| Sky Soft (primary container) | `#A8D8E8` |
| Sage (secondary) | `#7DBE9A` |
| Cream (surface) | `#F4F1DE` |
| Ink (on-surface) | `#0B1F26` |

Concept art themes (5 hero images bundled in `assets/concept_art/`): Zen · Mindfulness · Inner Child · Inner Peace · Empowerment.

## Licence

MIT — see `LICENSE`.
