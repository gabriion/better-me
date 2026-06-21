# Garmin Connect — developer setup

Better Me uses the Garmin Health API to drive the Daily Tips engine.

## 1. Apply for developer access

1. Go to <https://developer.garmin.com/gc-developer-program/>
2. Request **Health API** access. Approval takes ~1 week.
3. Once approved, Garmin issues:
   - **Consumer Key**
   - **Consumer Secret**

## 2. Local configuration

Create `local.properties` (already git-ignored) and add:

```properties
GARMIN_CONSUMER_KEY=...
GARMIN_CONSUMER_SECRET=...
```

These are read at build time and injected into `BuildConfig`. The app stores user OAuth tokens in EncryptedSharedPreferences after the user completes the in-app OAuth flow.

## 3. OAuth1.0a flow

Garmin Health API uses OAuth1.0a (three-legged):

1. App requests **request token** via `POST https://connectapi.garmin.com/oauth-service/oauth/request_token`
2. App launches Chrome Custom Tab pointing to `https://connect.garmin.com/oauthConfirm?oauth_token=...`
3. User authorises in Garmin Connect; Garmin redirects to `betterme://garmin-callback?oauth_token=...&oauth_verifier=...`
4. App exchanges verifier for **access token** via `POST https://connectapi.garmin.com/oauth-service/oauth/access_token`
5. Access token is stored encrypted on-device.

## 4. Endpoints consumed by V1

| Endpoint | Used by |
|---|---|
| `/wellness-api/rest/dailies` | Sleep, steps, RHR, stress, HRV daily summaries |
| `/wellness-api/rest/sleep` | Detailed sleep stages |
| `/wellness-api/rest/activities` | Recent activities with avg/max HR |

## 5. Background sync

`GarminSyncWorker` (WorkManager, daily at 06:00 local) pulls the previous day's summary and persists it to Room. The tip engine evaluates on cached data, so the app remains fully functional offline.

## Status

⏳ **Awaiting Garmin developer approval.** Until credentials arrive, `GarminClient` resolves to `GarminClientStub` and the tip engine emits default mindfulness tips.
