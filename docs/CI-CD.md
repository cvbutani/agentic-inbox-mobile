# CI/CD — Release Build workflow

`.github/workflows/release.yml` builds and optionally publishes the app. Trigger it from
**GitHub → Actions → Release Build → Run workflow**.

## Inputs

| Input | What it does |
|---|---|
| *Use workflow from* (branch dropdown) | The branch to build from. The version bump commit is pushed back to this branch. |
| `version_bump` | `major`, `minor`, `bugfix`, `build-number`, or `none`. The first three bump `versionName` in `gradle.properties` / `MARKETING_VERSION` in the Xcode project **and** increment `versionCode` / `CURRENT_PROJECT_VERSION` by 1. `build-number` keeps the version name and only increments the build number (e.g. re-uploading the same version to the stores). `none` builds with the current version untouched. |
| `platform` | `both`, `android`, or `ios`. |
| `deploy` | When checked, uploads the AAB to the Play Store **internal** track and the IPA to **TestFlight** via fastlane. When unchecked, the AAB/IPA are only attached as workflow artifacts. |

Versioning note: the iOS app uses a generated Info.plist (`GENERATE_INFOPLIST_FILE = YES`),
so the version source of truth is `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` in
`iosApp/iosApp.xcodeproj/project.pbxproj` — that is what the pipeline updates
(`scripts/bump_version.sh`), and Xcode writes it into the built app's Info.plist.

## Required repository secrets

Set these in **Settings → Secrets and variables → Actions**.

### Android

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Release keystore, base64-encoded: `base64 -w0 release.keystore` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |
| `PLAY_STORE_JSON_KEY_BASE64` | Google Play service-account JSON key, base64-encoded. Create it in Google Cloud Console, then grant it access in Play Console → Users and permissions. Only needed when `deploy` is checked. |

### iOS

| Secret | Value |
|---|---|
| `IOS_CERT_BASE64` | Apple Distribution certificate (.p12), base64-encoded |
| `IOS_CERT_PASSWORD` | Password of the .p12 |
| `IOS_PROFILE_BASE64` | App Store provisioning profile (.mobileprovision) for `com.sonicstarsolutions.agentic.inbox`, base64-encoded |
| `IOS_PROFILE_NAME` | The provisioning profile's name exactly as it appears in the Apple Developer portal |
| `APPLE_TEAM_ID` | 10-character Apple Developer team ID |
| `ASC_KEY_ID` | App Store Connect API key ID |
| `ASC_ISSUER_ID` | App Store Connect API issuer ID |
| `ASC_KEY_CONTENT` | Contents of the App Store Connect API .p8 key, base64-encoded |

## First-time store setup (one-off, manual)

- **Play Store**: the very first AAB for an app cannot be uploaded by the API — create the
  app in Play Console and upload one build manually, then the pipeline can publish to the
  internal track.
- **App Store Connect**: create the app record (bundle ID `com.sonicstarsolutions.agentic.inbox`)
  before the first TestFlight upload.

## How it runs

1. **version** job (ubuntu): runs `scripts/bump_version.sh`, commits the bumped
   `gradle.properties` + `project.pbxproj` back to the selected branch, and exposes the
   commit SHA so both build jobs compile exactly that commit.
2. **android** job (ubuntu): builds `:androidApp:bundleRelease` signed with the keystore
   from secrets, uploads the AAB artifact, and — if `deploy` — runs `fastlane android internal`.
3. **ios** job (macos-15): installs the cert + profile into a temp keychain, builds the IPA
   with `fastlane ios beta` (the Xcode build phase compiles the shared KMP framework, which
   is why the job also installs a JDK), uploads the artifact, and — if `deploy` — pushes the
   build to TestFlight.
