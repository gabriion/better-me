# Health Connect setup

Better Me reads biometrics from **Android Health Connect** — Google's vendor-agnostic on-device health platform. Health Connect aggregates data from Garmin Connect, Samsung Health, Fitbit, Google Fit, Whoop, Mi Fit and many other fitness apps. The user simply grants per-record-type permissions in the system Health Connect UI.

Why Health Connect rather than the Garmin Health API directly? Garmin's developer programme is **business-gated** (only companies are approved). Health Connect is free, open to any Android app, and works with any wearable the user already has — so Better Me supports far more devices than a Garmin-only path would have.

## What we read

| Record type | Used by |
|---|---|
| `SleepSessionRecord` | "You slept Xh — consider a power nap" tips |
| `StepsRecord` | "Only X steps so far — a walk would help" tips |
| `RestingHeartRateRecord` | RHR-trend rules in the tip engine |
| `HeartRateVariabilityRmssdRecord` | HRV recovery tips |
| (future) `ExerciseSessionRecord` | Sport-typed Goals auto-progress |

## On the user's device

1. The user installs Better Me.
2. On first run we check if Health Connect is available. On Android 14+ it's a system component; on older devices it's a free APK from Play Store.
3. When the user enables "Connect health data" in Settings, we launch the system Health Connect permission screen and request the read scopes above.
4. The user can revoke at any time — we degrade gracefully to app-internal signals only.

## App-internal fallback

When Health Connect is unavailable or unauthorised, the `AppSignalsSource` derives meaningful tips from the user's own logged data:
- Days since last workout
- Workout streak length
- Weight trend over the last 28 days
- Days since last weigh-in

This means the app is fully useful from day one even without a tracker.

## Implementation

See:
- `app/src/main/java/com/gabriion/betterme/health/HealthDataSource.kt` — interface
- `app/src/main/java/com/gabriion/betterme/health/HealthConnectSource.kt` — Health Connect reads
- `app/src/main/java/com/gabriion/betterme/health/AppSignalsSource.kt` — DAO-derived signals
- `app/src/main/java/com/gabriion/betterme/health/CombinedHealthSource.kt` — the bound composite

## Status

✅ **Implemented and bound.** The permission-request UI (system Health Connect dialog) is a follow-up — the data source returns `HealthSnapshot.EMPTY` until the user opts in, and the tip engine surfaces app-internal signals in the meantime.
