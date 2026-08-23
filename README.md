# Agentic Inbox

A native Android + iOS email client for the [`cloudflare/agentic-inbox`](https://github.com/cloudflare/agentic-inbox) Worker backend, built with Kotlin Multiplatform and Compose Multiplatform. One shared codebase renders the entire UI on both platforms; the backend is used as-is with no server changes.

You point the app at your own Worker deployment — mail, credentials, and cache stay between your device and your Cloudflare account.

<a href="https://apps.apple.com/us/app/agentic-inbox/id6793996922">
  <img alt="Download on the App Store" align="middle" width="150" height="50"
       src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg">
</a>
<a href="https://play.google.com/store/apps/details?id=com.sonicstarsolutions.agentic.inbox">
  <img alt="Get it on Google Play" align="middle" width="191" height="74"
       src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png">
</a>

## Features

- **Mailboxes** — list, create (against your Worker's allowed domains), switch, delete
- **Inbox** — threaded email list per folder, pagination, pull-to-refresh, swipe to archive/delete with undo, multi-select batch actions (read/unread, archive, delete)
- **Folders** — system folders (Inbox, Sent, Drafts, Archive, Spam, Trash) plus custom folders with create/rename/delete
- **Conversation view** — expandable message cards, sanitized HTML rendering with remote-image blocking, star/read toggles, move to folder, attachments
- **Compose** — new, reply, reply-all, forward; drafts save locally and resume in their original mode
- **Search** — debounced search-as-you-type with filters (from, to, subject, date range, read/starred/attachment), removable filter chips, infinite scroll
- **Offline cache** — Room-backed local store with optimistic UI for common actions
- **Adaptive layouts** — list-detail split on tablets, responsive onboarding and mailbox grid, light/dark theme

Out of scope for v1: the web client's AI agent panel, push notifications, MCP integration.

## Architecture

Two-module layout on the modern KMP plugin (`com.android.kotlin.multiplatform.library`, AGP 9):

```
androidApp/          Thin Android shell — AppActivity.setContent { App() }
sharedUI/            KMP library: all Compose UI, domain, and data
  commonMain/
    data/network/    Ktor client + Ktorfit API, DTOs, Cloudflare Access auth headers
    data/local/      Room (KMP): entities, DAOs, migrations
    data/repository/ Offline-first single source of truth
    domain/model/    Email, Thread, Folder, Mailbox, Draft, SearchQuery, …
    domain/usecase/  One class per operation (SendEmail, SearchEmails, MoveEmail, …)
    ui/              One package per screen + shared components
    navigation/      Navigation 3 host — explicit callback wiring, no reactive routing
    di/              Koin modules
  commonTest/        ViewModel, repository, and formatter tests with hand-rolled fakes
iosApp/              Xcode project consuming sharedUI as a static framework
```

Each screen is a `Screen` composable backed by a `ViewModel` exposing a single `StateFlow<UiState>`. ViewModels talk to use cases; use cases talk to repositories; repositories merge the Ktor API with the Room cache.

**Stack:** Kotlin 2.4 · Compose Multiplatform 1.11 · Ktor 3.5 + Ktorfit · Room 2.8 (KMP) · Koin 4.1 · Navigation 3 · kotlinx-{coroutines, serialization, datetime}

## Getting started

### Prerequisites

- A deployed [`agentic-inbox`](https://github.com/cloudflare/agentic-inbox) Cloudflare Worker
- A Cloudflare Access **service token** (Client ID + Client Secret) for that Worker
- JDK 17+, Android Studio (latest stable); Xcode for the iOS target (macOS only)

### Getting an Access service token

The app authenticates to your Worker with a Cloudflare Access **service token**:

1. In the Cloudflare dashboard, open **Zero Trust → Access → Service auth → Service tokens** and click **Create Service Token**. Give it a name and a duration.
2. Copy the **Client ID** and **Client Secret** immediately — the secret is shown only this once.
3. In **Zero Trust → Access → Applications**, open the application protecting your Worker and add a policy with the **Service Auth** action that includes your new token (without this policy the token authenticates but is still denied).

On first launch the app asks for your Worker URL, the Client ID, and the Client Secret. They are stored only on the device.

## Development conventions

- **TDD** — new behavior starts with a failing test in `sharedUI/src/commonTest`. ViewModels are tested against the fakes in `testutil/FakeRepositories.kt`; pure logic (time formatting, layout selection) is extracted into testable functions rather than asserted through Compose UI tests.
- **Design system** — [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) defines the 72dp list grid, unread/selected conventions, token-only colors (no alpha-mixed values), 48dp touch-target floor, and the shared `StatusPane` / `ErrorBanner` / `InitialsAvatar` components. Check it before styling anything.
- **Adaptive layouts** — screens measure themselves against the shared `WindowWidthClass` breakpoints (600dp / 840dp) rather than inventing their own cutoffs.

## Project docs

- [agentic-inbox-cmp-plan.md](agentic-inbox-cmp-plan.md) — original scope and architecture plan
- [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) — UI conventions all screens follow
- [CONTRIBUTING.md](CONTRIBUTING.md) — how to build, test, and submit changes

## License

Copyright © 2026 Chirag Butani (Sonic Star Solutions). The source code is licensed under the
[Apache License 2.0](LICENSE).

**The "Agentic Inbox" name and the app icon are not covered by this license.** Forks and
derived apps must ship under their own name and branding — please don't publish builds that
could be mistaken for the official app.

The project began from the [Kotlin Multiplatform template](https://github.com/Kotlin/KMP-App-Template)
scaffold (Apache 2.0); it has since been substantially rewritten.
