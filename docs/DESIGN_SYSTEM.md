# Design System — Agentic Inbox (Airmail)

Conventions every screen must follow. All values reference Material 3 theme tokens
(`MaterialTheme.colorScheme` / `MaterialTheme.typography`) or the extended Airmail tokens below;
never hardcode colors, alphas, or ad-hoc sizes.

> **Status:** phases 1 to 6 of [AIRMAIL_MIGRATION_PLAN.md](AIRMAIL_MIGRATION_PLAN.md) are
> implemented; phase 7 (device walk on phone and tablet, iOS check, store screenshots) is still
> open, and this note goes when it closes. New UI work follows this document. The visual reference is [design/airmail-mock.html](../design/airmail-mock.html):
> every screen, phone and tablet, light and dark.

## Direction

Each conversation is a letter: navy ink on pale blue paper. Three things carry the identity, and
nothing else is added for decoration.

1. **Paper and ink.** Blue-tinted neutral surfaces with true navy `#1D3557` as `primary`.
2. **The airmail band.** A 6dp strip of navy and red diagonal stripes.
3. **The postmark.** A 40dp ring holding a day and month, wherever a date leads a group.

The logo is the same identity as a mark: an airmail-bordered envelope in navy and red, with the
band's 45° stripes. Files, versions and rules are in [design/logo/LOGO_GUIDE.md](../design/logo/LOGO_GUIDE.md).

Material 3 governs structure, navigation, and controls on both Android and iOS. The identity is
expressed through theming and these two components, never through custom controls.

## Color

### Material roles (`theme/Color.kt`)

| Role | Light | Dark |
|---|---|---|
| `primary` | `#1D3557` | `#AFC8F0` |
| `onPrimary` | `#FFFFFF` | `#0F2A4D` |
| `primaryContainer` | `#D6E2F5` | `#2B446B` |
| `onPrimaryContainer` | `#12284A` | `#D6E2F5` |
| `secondary` | `#3A5B85` | `#A9C4EA` |
| `onSecondary` | `#FFFFFF` | `#0E2C52` |
| `secondaryContainer` | `#DCE6F4` | `#283A55` |
| `onSecondaryContainer` | `#14305A` | `#D3E0F3` |
| `tertiary` | `#7A5200` | `#F2C14E` |
| `onTertiary` | `#FFFFFF` | `#3F2C00` |
| `tertiaryContainer` | `#FFE2A8` | `#5A3F00` |
| `onTertiaryContainer` | `#2A1A00` | `#FFE2A8` |
| `error` | `#BA1A1A` | `#FFB4AB` |
| `onError` | `#FFFFFF` | `#690005` |
| `errorContainer` | `#FFDAD6` | `#93000A` |
| `onErrorContainer` | `#410002` | `#FFDAD6` |
| `background`, `surface` | `#F4F7FB` | `#0F1620` |
| `onBackground`, `onSurface` | `#17212F` | `#E3E8F0` |
| `surfaceVariant` | `#DCE3EE` | `#364256` |
| `onSurfaceVariant` | `#44536A` | `#B4C0D1` |
| `surfaceDim` | `#D5DBE5` | `#0F1620` |
| `surfaceBright` | `#F4F7FB` | `#333F52` |
| `surfaceContainerLowest` | `#FFFFFF` | `#0A1018` |
| `surfaceContainerLow` | `#EEF2F8` | `#151E2B` |
| `surfaceContainer` | `#E8EDF5` | `#1A2433` |
| `surfaceContainerHigh` | `#E1E7F0` | `#212C3D` |
| `surfaceContainerHighest` | `#DAE1EC` | `#2A3649` |
| `outline` | `#73829A` | `#8593A6` |
| `outlineVariant` | `#C8D2E0` | `#364256` |
| `inverseSurface` | `#2B3544` | `#E3E8F0` |
| `inverseOnSurface` | `#EDF1F7` | `#2B3544` |
| `inversePrimary` | `#AFC8F0` | `#1D3557` |
| `scrim` | `#000000` | `#000000` |

### Extended Airmail tokens (`MaterialTheme.airmail`)

Three colors Material has no role for. They live in `AirmailColors`, provided through a
composition local next to the color scheme.

| Token | Light | Dark | Used for |
|---|---|---|---|
| `airmailNavy` | `#1D3557` | `#AFC8F0` | Band stripes only |
| `airmailRed` | `#C8372D` | `#F0958C` | Band stripes only |
| `star` | `#B87A00` | `#F2C14E` | A starred star icon |

### What each color means

- **Navy (`primary`)**: the brand, unread time, the primary action, links, the postmark.
- **`primaryContainer`**: a selected row. Nothing else tints a row.
- **`secondaryContainer`**: the current drawer destination and active filter chips.
- **Amber (`tertiaryContainer`, `star`)**: things waiting on time or kept on purpose: scheduled
  status, the Draft label, a starred star.
- **Red (`error`, `errorContainer`)**: errors only. Unread mail, counts, and drafts are never red.
- **`airmailRed`**: exists only inside the band, always beside navy. Never a fill, never text,
  never a state.

### Pairing and muting

- A container color always pairs with its own "on" color: `onPrimaryContainer` on
  `primaryContainer`, `onErrorContainer` on `errorContainer`. Never `onSurface` on a container
  fill (breaks in dark mode). A selected row sets all of its text, snippet and time included, to
  `onPrimaryContainer`.
- Secondary text: `onSurfaceVariant`. Decorative/empty-state icons: `outlineVariant`.
- Never `Color.copy(alpha = …)` on a token — pick the right token instead.
- Unread counts are plain `labelMedium` text in `onSurfaceVariant`, capped at "99+".
- Text pairs meet 4.5:1 and icon-only state colors meet 3:1 against their surface, in both
  schemes. `ColorContrastTest` enforces the table above.

## Typography

Open Sans in every slot of Material 3's default type scale (`theme/Typography.kt`). Map text to
roles; never hand-pick a size.

- **Two weights in the UI: `Normal` and `SemiBold`.** Weight is a state channel (unread), so it
  is not spent on decoration. Never `Bold` except the postmark's month, which is 10sp.
- Numerals are tabular (Open Sans default), so times and dates align in a column.

| Element | Style |
|---|---|
| App bar title | `titleLarge`, `SemiBold`, `primary` |
| Thread subject, onboarding headline | `headlineSmall` / `headlineMedium`, `SemiBold` |
| Row sender | `titleMedium`; `SemiBold` when unread |
| Row subject | `bodyMedium`; `SemiBold` when unread |
| Row snippet | `bodySmall`, `onSurfaceVariant`, two lines |
| Row time | `labelMedium`; `SemiBold` + `primary` when unread |
| Message card sender | `titleSmall`, `SemiBold` |
| Message body | `bodyLarge` |
| Button, chip, drawer item | `labelLarge` |
| Day header name | `titleSmall`, `SemiBold`, `primary` |
| Avatar initials | `titleSmall`, `SemiBold` |

## Shape and elevation

- Message cards 12dp, chips and attachment tiles 8dp, text fields 4dp, FAB 16dp, drawer 16dp on
  its trailing corners. Shape is always set on a `Surface(shape = …)`, never a `clip` modifier.
- Depth is tonal: surfaces step through the `surfaceContainer*` roles. The only shadow in the app
  is the FAB's own Material elevation.

## Signature components

### `AirmailBand`

- 6dp tall, full width, drawn on a `Canvas`. Stripes run at -45°: navy 12dp, gap 6dp, red 12dp,
  gap 6dp, repeating. Gaps show the surface behind the band.
- **It is the app's progress line.** `AirmailBand(active = true)` slides the stripes one period
  every 500ms, linear, for as long as work is in flight (refresh, background sync, sending). At
  rest it is static. With system animations off it does not move; it dims instead.
- **Where it goes, and nowhere else:** directly under the top app bar on the inbox and on the
  composer, and across the top of onboarding. Not on the thread, search, settings, dialogs, or
  cards. More than one band on a screen is always wrong.
- The Material pull-to-refresh indicator stays for the drag gesture itself.

### `Postmark`

- A 40dp ring: 1.5dp `primary` stroke, no fill, `primary` content. Day of month on top
  (14sp `SemiBold`), three-letter uppercase month below (10sp `Bold`, +0.04em tracking).
- It occupies the 40dp leading slot, so the text beside it starts on the 72dp edge.
- It marks **a date that leads a group or an event**: day headers in mail lists, day separators
  between messages in a thread, the send day of a scheduled message. It is never a decoration
  and never repeats inside the group it heads.

### `DayHeader`

- `Postmark` + day name ("Today", "Yesterday", weekday name inside a week, then "3 July", with
  the year once it differs from the current one) + optional trailing "3 unread" count in
  `labelMedium` / `onSurfaceVariant`.
- Padding `16dp` horizontal, `12dp` top, `4dp` bottom. Sticky in lists (`stickyHeader`), with a
  `surface` background so rows pass under it. Static between messages in a thread.
- Groups come from `EmailDayGrouper` (pure, unit-tested), never computed inside a composable.

## List grid

- **72dp text edge**: every list row is `40dp leading slot + 16dp gutter` inside `16dp`
  horizontal padding, so all text starts at 72dp — and every `HorizontalDivider` uses
  `padding(start = 72.dp)` to match. Snippets, subjects, and senders share this single left
  edge; nothing sits outside it. The leading slot holds an avatar, a postmark, or an icon.
- **Row padding**: `16dp` horizontal, `12dp` vertical. Intra-row gaps: `2dp` between text
  lines, `8dp` between text and trailing controls.
- Applies to `EmailListItem`, `DraftListItem`, `SkeletonEmailRow`, scheduled rows, mailbox rows,
  and `DayHeader` alike: same list, same grid, regardless of content type.

## Unread & selected

- **Unread** = `FontWeight.SemiBold` sender/subject + time in `primary`. Never `Bold`, never a
  background tint, never a dot or badge.
- **Selected** = `primaryContainer` background. This is the only state that tints a row, so
  selection and unread never share a visual channel. In selection mode a selected row's avatar
  becomes a `primary` circle with a check; unselected rows keep their avatar.

## Avatars and labels

- `InitialsAvatar` everywhere an identity appears (inbox rows, message cards, drawer header,
  mailbox picker, contact suggestions). Fixed tonal pairs — pastel container fill + deep same-hue
  text, every pair ≥ 4.5:1 — hashed from the name so identity color is stable across screens and
  themes.
- `LabelChip` uses the fixed pairs in `LabelColors.kt` in both themes. At most three chips on a
  row, then "+N". In the drawer a label is a 14dp dot: pastel fill with a 2dp ring in the pair's
  deep color, so it reads on light and dark.

## Time

- One formatter object, `EmailTimeFormatter` (util package, fully unit-tested).
  - Rows under a `DayHeader` show the clock time only (`formatClock`); the header carries the day.
  - Rows with no header (search results, collapsed message cards) use `format`: clock time
    inside 24h, then "Yesterday", weekday inside a week, then "Jul 3" / "Jul 3, 2025".
  - An expanded message card shows `formatAbsolute`.
- Scheduled send times come from `ScheduleTimeFormatter`.
- No relative "3m/2h" strings anywhere.

## Interaction

- Touch targets never below 48dp — keep glyphs small via `Icon.size`, never by shrinking
  the `IconButton`.
- The screen's primary action is a labeled `ExtendedFloatingActionButton`, not a bare icon:
  Compose on the inbox, Send in the composer, New mailbox in the picker. One per screen.
- Top bars use the `surface` color and hide on scroll via `enterAlwaysScrollBehavior` wired
  through `Modifier.nestedScroll`; the band, where present, travels with the bar. Mode switches
  (normal ↔ selection) `Crossfade`, never hard-swap. The selection bar uses `surfaceContainer`.
- At most 3 icon actions in an app bar; the rest go in an overflow menu with text labels. Thread:
  Archive, Delete, overflow (Move to folder, Mark as spam). Selection: Archive, Delete, overflow.
- List mutations animate via `Modifier.animateItem()`.
- Destructive actions are staged with an Undo snackbar or confirmed in a dialog, never silent.

## Detail cards (thread screen)

- Message cards: 16dp horizontal gutter, 12dp radius via `Surface(shape=)` (never a clip
  modifier), `surfaceContainerLow` fill — and exactly **one** surface per card: anything
  rendered inside (including themed HTML bodies) uses the card's own color.
- Labeled actions: icon-only buttons are allowed only in app bars and for the star and
  read toggles in a card header. Inside content, actions carry text labels (`TextButton` with a
  small leading icon), start-aligned — never `SpaceEvenly` stretched.
- Expanding content animates (`AnimatedVisibility`, expand/shrink + fade); nothing pops.
- A `DayHeader` separates messages sent on different days.

## Forms and the composer

- The composer uses borderless rows split by hairline `outlineVariant` dividers. A message is
  not a settings form.
- Real forms (onboarding, mailbox settings, dialogs) use `OutlinedTextField` with a label and,
  where it helps, supporting text. Errors that belong to a form show in the shared `ErrorBanner`,
  inline and persistent, not in a snackbar.

## States

- **Loading (first page)**: skeleton rows on the 72dp grid (`surfaceContainerHigh` shapes),
  not a centered spinner. This includes the composer while a reply or draft is prefilled.
- **Refreshing / syncing / sending**: the airmail band slides, on screens that have one.
- **Empty / error**: always the shared `StatusPane` component (icon in `outlineVariant` +
  headline + optional detail + optional action). Errors that replace an empty screen get a
  Retry action; errors over existing content stay in a snackbar. No narration of the
  obvious — no "End of list" markers.
- **Snackbars**: one glanceable line; truncate embedded subjects to ~30 chars.

## Adaptive layout

- Screens measure themselves against the shared `WindowWidthClass` breakpoints (600dp / 840dp).
- Navigation is a modal drawer at every width; there is no rail and no persistent drawer.
- On expanded width the inbox is a list-detail split. The open row takes the selection tint and
  the detail pane closes with an X, not a back arrow.
- Reading and writing surfaces are capped and centered: thread and search 840dp, composer 720dp,
  onboarding form 460dp.

## Accessibility

- 48dp targets, 4.5:1 text, 3:1 for state-carrying icons, in both themes.
- State is never carried by color alone: unread has weight, selected has a check, starred has a
  filled glyph, scheduled has text.
- Every icon-only control has a `contentDescription`; decorative icons pass `null`.
- Layouts hold at font scale 1.3; nothing is clipped to a fixed height that text can outgrow.
