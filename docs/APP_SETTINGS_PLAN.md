# Plan: app-level Settings and backend requirement notice

Status: **draft, not started.** Items marked **(decide)** need an answer before the phase they gate.

## Why

Per-mailbox settings exist (`MailboxSettingsScreen`) but the app has no app-level surface. Nothing in the app
says which backend it needs. Onboarding shows only the placeholder `https://your-worker.workers.dev`, and the
sign-out dialog still says "Worker URL". A user who deploys upstream `cloudflare/agentic-inbox` sees broken
labels, bulk actions and send-later and blames the app.

[PUBLIC_BACKEND_MIGRATION.md](PUBLIC_BACKEND_MIGRATION.md) already decided that agentic-inbox-public is the only
supported backend, and its item 1.5 ("settings copy names public as the supported backend, with the minimum
version") was never shipped. This plan finishes 1.5 and gives it a home.

## Goal and rules

- Anyone installing the app learns, before and after connecting, that it needs the Sonic Star fork of
  agentic-inbox and where to get it.
- Small and shippable: no server changes, no new network calls in Phase 1-2.
- Repo conventions: failing test first (TDD), `DESIGN_SYSTEM.md` tokens, 48dp targets, no "PR #n" in commit
  messages.

### Decisions

1. **Wording (decide): "requires" or "works best with".** The migration plan says Worker-only code was deleted,
   so upstream is unsupported, not merely worse. Recommendation: **"Requires"**. Use "works best with" only if
   you want to leave room for upstream to work in a degraded way, in which case Phase 3 becomes necessary
   rather than optional.
2. **Repo link (decide).** The README links `https://github.com/SonicStar-Solutions/agentic-inbox`. Use that
   as the single constant unless the slug is wrong.
3. **Where Sign out lives (decide).** Recommendation: move it from the picker app bar into Settings. The picker
   bar then shows one settings icon, not sign-out, so a mis-tap can no longer reach it. Alternative: keep both
   icons (2 of the 3 allowed in an app bar).

## Phase 1 - Copy and constants (about half a day)

Smallest useful change, and it fixes the stale Worker wording.

| # | Change | Test first |
|---|---|---|
| 1.1 | Add `BackendInfo` (`name`, `repoUrl`, `minimumVersionNote`) as one commonMain constant, the single source for all copy below. | `BackendInfoTest` asserts the URL is https and non-blank |
| 1.2 | Onboarding: under the URL field, replace/supplement the `workers.dev` hint with "Requires a deployed agentic-inbox" plus a tappable link. Open it with the platform URI handler; no in-app browser. | `OnboardingViewModelTest` (state exposes the info), screenshot check |
| 1.3 | Sign-out dialog: say "backend URL", not "Worker URL". | none (copy) |
| 1.4 | README and `PUBLIC_BACKEND_MIGRATION.md` progress table: mark 1.5 done and point to this plan. | n/a |

Exit: onboarding names the backend and links to it. No "Worker" wording left in user-facing strings
(`grep -rn "Worker" sharedUI/src/commonMain` shows only code comments).

## Phase 2 - App Settings screen (1-2 days)

New destination `AppSettings` (a `data object`, registered in `appNavConfiguration`; every destination needs its
`subclass` registration so the back stack survives process death on iOS too).

**Entry:** settings icon in the `MailboxPickerScreen` top bar (48dp target, `contentDescription = "Settings"`).
Per the design system, at most 3 icon actions; this uses one (two if Decision 3 keeps sign-out in the bar).

**Layout** (sections in a single scrolling column, capped and centered on wide windows like the other form
screens (cap to be picked from the design system's 460-840dp range); borderless list rows with hairline dividers, `ErrorBanner` for errors):

1. **Backend**: a card with the name, one line of explanation ("Agentic Inbox needs a deployed agentic-inbox
   backend"), the connected URL (from `Credentials.baseUrl`; the secret is never displayed), and an
   "Open repository" `TextButton` with a leading icon.
2. **Mailbox defaults**: swipe actions. `SwipeSettingsViewModel` already exists but is only reachable from
   mailbox settings; link to the same screen or move the controls here. Do not duplicate state.
3. **About**: app version, privacy policy link (`PRIVACY.md` / `docs/privacy.html`), licenses.
4. **Account**: Sign out, reusing the existing confirm dialog and `ClearCredentialsUseCase`.

| # | Change | Test first |
|---|---|---|
| 2.1 | `AppSettingsViewModel`: exposes `BackendInfo`, masked connected host, app version; `signOut()` calls `ClearCredentialsUseCase`. | `AppSettingsViewModelTest` with a fake credentials repository: host shown without the secret, sign-out clears |
| 2.2 | `AppInfo` interface (`versionName`) provided per platform through Koin. Android reads `PackageInfo`, iOS reads `CFBundleShortVersionString`; there is no shared version source today (Android sets `versionName` in `androidApp/build.gradle.kts`). | fake `AppInfo` in the view-model test |
| 2.3 | `AppSettingsScreen` plus the navigation entry and `AppNavHost` wiring. Sign-out navigation reuses the picker's existing handling. | UI state tests; manual run on emulator-5554 |
| 2.4 | Picker app bar: add the settings icon (and remove sign-out if Decision 3 says so). | `MailboxPickerViewModelTest` unchanged; manual check |
| 2.5 | Demo data mode (`DemoDataModule`): make sure the screen renders with the demo credentials, for screenshots and store listings. | manual |

Exit: Settings opens from the picker on phone and tablet, shows the backend notice and link, sign-out still
works and still confirms before discarding credentials.

## Phase 3 - Compatibility check (optional, not recommended yet)

Only worth it if users connect to the wrong backend in practice.

- Phase 2 of the migration plan dropped capability discovery, and `GET /api/v1/config` returns only `domains`
  and `emailAddresses`. A check needs a small change in agentic-inbox-public first: add `version` (and maybe
  `features`) to `/config`, plus a route test.
- Then Mobile reads it on connect and, if it is missing or below the minimum, shows a non-blocking banner in
  Settings ("This server looks older than the app needs") rather than refusing to connect.
- Cheaper stopgap without server changes: if `/config` lacks the new key, treat the server as "unknown" and
  show nothing. Do not guess from failed requests.

## Risks and notes

- The link opens an external page, so confirm it is acceptable under the store listings' stated data practices;
  `PRIVACY.md` should still say the app only talks to the user's own backend.
- Version display needs a real iOS bundle value; if it is blank in debug builds, fall back to "dev".
- Copy must stay consistent with the README. Keep the constant in one place and have the README quote it.

## Rough size

Phase 1: about half a day. Phase 2: 1-2 days. Phase 3: about a day split across both repos, only if needed.

## Progress

| Phase | Status | Commit |
|---|---|---|
| 1 Copy and constants | in progress | |
| 2 App Settings screen | not started | |
| 3 Compatibility check | deferred | |
