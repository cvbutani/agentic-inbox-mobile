# Design System — Agentic Inbox

Conventions every screen must follow. All values reference Material 3 theme tokens
(`MaterialTheme.colorScheme` / `MaterialTheme.typography`); never hardcode colors, alphas,
or ad-hoc sizes.

## List grid

- **72dp text edge**: every list row is `40dp leading slot + 16dp gutter` inside `16dp`
  horizontal padding, so all text starts at 72dp — and every `HorizontalDivider` uses
  `padding(start = 72.dp)` to match. Snippets, subjects, and senders share this single left
  edge; nothing sits outside it.
- **Row padding**: `16dp` horizontal, `12dp` vertical. Intra-row gaps: `2dp` between text
  lines, `8dp` between text and trailing controls.
- Applies to `EmailListItem`, `DraftListItem`, and `SkeletonEmailRow` alike: same list, same
  grid, regardless of content type.

## Unread & selected

- **Unread** = `FontWeight.SemiBold` sender/subject + time in `primary`. Never `Bold` (too
  heavy in Open Sans at list sizes), never a background tint.
- **Selected** = `primaryContainer` background. This is the only state that tints a row, so
  selection and unread never share a visual channel.

## Muted content

- Secondary text: `onSurfaceVariant`. Decorative/empty-state icons: `outlineVariant`.
- Never `Color.copy(alpha = …)` on a token — pick the right token instead.

## Color pairing

- A container color always pairs with its own "on" color: `onPrimaryContainer` on
  `primaryContainer`, `onErrorContainer` on `errorContainer`. Never `onSurface` on a
  container fill (breaks in dark mode).
- Unread counts are plain `labelMedium` text in `onSurfaceVariant`, capped at "99+". Red
  badges mean errors, not mail.

## Avatars

- `InitialsAvatar` everywhere an identity appears (inbox rows, drawer header, mailbox
  picker). Fixed tonal pairs — pastel container fill + deep same-hue text, every pair
  ≥ 4.5:1 — hashed from the name so identity color is stable across screens and themes.

## Time

- One formatter: `EmailTimeFormatter.format` (util package, fully unit-tested). Clock time
  inside 24h, then "Yesterday", weekday inside a week, then "Jul 3" / "Jul 3, 2025".
  No relative "3m/2h" strings anywhere.

## Interaction

- Touch targets never below 48dp — keep glyphs small via `Icon.size`, never by shrinking
  the `IconButton`.
- The screen's primary action is a labeled `ExtendedFloatingActionButton`, not a bare icon.
- Top bars hide on scroll via `enterAlwaysScrollBehavior` wired through
  `Modifier.nestedScroll`. Mode switches (normal ↔ selection) `Crossfade`, never hard-swap.
- At most 3 icon actions in an app bar; the rest go in an overflow menu with text labels.
- List mutations animate via `Modifier.animateItem()`.

## Detail cards (thread screen)

- Message cards: 16dp horizontal gutter, 12dp radius via `Surface(shape=)` (never a clip
  modifier), `surfaceContainerLow` fill — and exactly **one** surface per card: anything
  rendered inside (including themed HTML bodies) uses the card's own color.
- Labeled actions: icon-only buttons are allowed only in app bars. Inside content, actions
  carry text labels (`TextButton` with a small leading icon), start-aligned — never
  `SpaceEvenly` stretched.
- Expanding content animates (`AnimatedVisibility`, expand/shrink + fade); nothing pops.

## States

- **Loading (first page)**: skeleton rows on the 72dp grid (`surfaceContainerHigh` shapes),
  not a centered spinner.
- **Empty / error**: always the shared `StatusPane` component (icon in `outlineVariant` +
  headline + optional detail + optional action). Errors that replace an empty screen get a
  Retry action; errors over existing content stay in a snackbar. No narration of the
  obvious — no "End of list" markers.
- **Snackbars**: one glanceable line; truncate embedded subjects to ~30 chars.
