# Contributing to Agentic Inbox

Thanks for your interest! This is a small project with a few firm conventions — following them
keeps reviews fast.

## Setup

You need a deployed [`agentic-inbox`](https://github.com/cloudflare/agentic-inbox) Worker and a
Cloudflare Access service token to run the app against real data (see the README). Building and
testing need neither:

```sh
# Shared-code unit tests (the main suite)
./gradlew :sharedUI:testAndroidHostTest

# Debug build on a connected device/emulator
./gradlew :androidApp:installDebug
```

iOS builds from `iosApp/iosApp.xcodeproj` on macOS.

## Conventions

1. **Tests first.** New behavior starts with a failing test in `sharedUI/src/commonTest`.
   ViewModels test against the fakes in `testutil/FakeRepositories.kt`; logic that would need a
   Compose UI test is instead extracted into a plain testable function (see `EmailTimeFormatter`,
   `onboardingLayoutFor`). Pure styling changes have no test surface — say so in the PR and
   include a screenshot.
2. **Design system.** All UI follows [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) — the 72dp
   list grid, token-only colors (no `Color.copy(alpha=…)`), 48dp touch-target floor, and the
   shared components (`StatusPane`, `ErrorBanner`, `InitialsAvatar`). If a change needs a new
   convention, propose the doc change in the same PR.
3. **Adaptive layouts** measure against the shared `WindowWidthClass` breakpoints — don't invent
   new width cutoffs.
4. **Dependencies** are deliberate: prefer the standard library and existing deps. Pre-1.0
   libraries get quarantined behind a single file (see `ui/compose/AttachmentPicker.kt`).
5. **Commits** are imperative, scoped, and explain *why* in the body when it isn't obvious.

## Pull requests

- Keep PRs focused — one concern per PR.
- `./gradlew :sharedUI:testAndroidHostTest` must pass.
- UI changes: attach before/after screenshots (dark and light if the change touches color).

## Branding

The code is Apache 2.0, but the "Agentic Inbox" name and icon are not (see the README's License
section). Contributions are accepted under the project's license.
