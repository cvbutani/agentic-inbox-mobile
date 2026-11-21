# Agentic Inbox — Logo Guidelines

The logo is the **Airmail Envelope**: an envelope framed by the airmail border, with a closed flap.
Each conversation is a letter; the stripes are the same navy and red 45° stripes as the in-app
`AirmailBand` ([DESIGN_SYSTEM.md](../docs/DESIGN_SYSTEM.md)).

Masters are SVG in `final/svg/`. Every shape is a plain filled path: no live text, no strokes, no
clip paths, no filters. The wordmark is Open Sans SemiBold converted to outlines.

**Geometry:** the envelope is 224 × 144 units. The outer frame has radius 18, the paper opening
radius 12, and the flap's lower apex radius 12 (the upper edge of the flap stays sharp, because the
band is only 14 units thick). Stripes are 14 wide with 6 gaps at 45°, phased so the top-left and
bottom-right corner arcs each sit inside a single stripe.

## 1. Versions and files

| Use | File | Notes |
|---|---|---|
| Symbol, full colour | `svg/agentic-inbox-mark.svg` | Default. 48 px wide and up (digital), 12 mm and up (print) |
| Symbol, small-size cut | `svg/agentic-inbox-mark-small.svg` | Bolder stripes and flap for 16 to 48 px |
| Symbol, dark backgrounds | `svg/agentic-inbox-mark-reversed.svg` | Uses the dark-mode airmail tokens `#AFC8F0` and `#F0958C` |
| Horizontal lockup | `svg/agentic-inbox-horizontal.svg` | Headers, README, docs. 140 px wide and up |
| Stacked lockup | `svg/agentic-inbox-stacked.svg` | Posters, signage, splash. 120 px wide and up |
| Reversed lockups | `svg/agentic-inbox-horizontal-reversed.svg`, `…-stacked-reversed.svg` | On navy or near-black |
| One colour | `…-black.svg`, `…-white.svg`, `…-mono-1d3557.svg` | Mark, small cut, both lockups. Both stripe colours become one |
| App icon (rounded tile) | `svg/agentic-inbox-app-icon.svg` | Store listings, docs, social |
| App icon (full-bleed) | `svg/agentic-inbox-app-icon-square.svg` | iOS and Apple touch icon master; the OS rounds the corners |
| Favicon | `svg/agentic-inbox-favicon.svg` | Tile plus small-size cut |
| Android adaptive | `svg/android-ic_launcher_{foreground,background,monochrome}.svg` | 108 x 108 dp canvas, envelope inside the 66 dp safe zone |

Rasters are in `png/` (transparent), `android-res/` (drop-in launcher set, same file names as the
project uses today), `ios/AppIcon-1024.png` (flat RGB, no alpha), and `web/` (favicon.ico, PNG icon
set, `site.webmanifest`, `head-snippet.html`).

Ladder for small sizes: full mark at 48 px and up, small-size cut from 24 to 48 px, the tile favicon
for 16 to 32 px. Below 16 px use the tile and accept a texture, not detail.

## 2. Clear space

Keep clear space equal to **half the height of the envelope** on every side of the symbol, and the
same distance around a lockup. It scales with the logo; never use a fixed distance. Nothing sits
inside that zone, including the `AirmailBand`.

## 3. Colour

| Name | HEX | RGB | CMYK (naive conversion) | Role |
|---|---|---|---|---|
| Navy | `#1D3557` | 29, 53, 87 | 67 39 0 66 | Brand anchor, flap, wordmark, tile |
| Airmail red | `#C8372D` | 200, 55, 45 | 0 72 78 22 | Stripes only |
| Paper | `#F4F7FB` | 244, 247, 251 | 3 2 0 2 | Tile interior, light stripes |
| Navy, dark mode | `#AFC8F0` | 175, 200, 240 | 27 17 0 6 | Reversed navy |
| Red, dark mode | `#F0958C` | 240, 149, 140 | 0 38 42 6 | Reversed red |

CMYK values are mathematical conversions, not press-proofed. No Pantone match was chosen: pick one
against a physical swatch book before any print run.

Red is part of the stripes and never a fill, text colour or state colour, the same rule as the app.

**Approved pairings:** full colour on white or paper; reversed on navy `#1D3557`, `#0F1620` or any
dark surface; white on navy or on red; black on white; navy mono on paper. On photographs, use the
tile version or the white version on a calm area.

## 4. Type

Wordmark: Open Sans SemiBold (600), tracking about -0.5%, outlined. Open Sans is bundled in the app
(`composeResources/font/open_sans.ttf`) under the SIL Open Font Licence, which permits use in a
logo. Do not retype the wordmark; use the lockup files.

## 5. Don't

- Don't stretch, rotate, or recolour the stripes outside the palette.
- Don't add shadows, outlines, gradients or glows.
- Don't use the full mark below 48 px wide; switch to the small-size cut.
- Don't put the red stripes on a red surface; use the white or reversed version.
- Don't redraw or re-space the lockups.
- Don't pair it with a second band: the in-app `AirmailBand` and the logo never share a screen
  region (onboarding shows the logo below the band, with clear space).

## 6. Tests run

- Size ladder from 16 to 256 px, with the 16 / 32 / 48 px pixel test: passed with the small-size cut
  and tile favicon; the full mark intentionally degrades to a texture at 16 px.
- One colour (black, white, navy), reversed, greyscale, 2 px squint blur: the envelope silhouette and
  flap survive each.
- Mirrored and rotated 180°: no unintended reading (rotated it reads as an open envelope).
- Android: foreground inside the 66 dp safe zone under circle and squircle masks; monochrome layer
  works as a themed icon.
- Shelf check: not tested against real reference logos (the bundled library had none for email).
  By my own knowledge of well-known mail apps, none uses navy and red airmail stripes, but this is
  an unverified judgement; compare against current mail apps before relying on it.

## 7. Open items

1. **Trademark and image clearance.** No trademark registry search and no reverse-image search was
   run on the name or the logo. Have both done before wide use.
2. **Where the icon is applied.** `androidApp/src/main/res/mipmap-*` (copied from
   `final/android-res/`), `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon~ios.png` and the
   root `AppIcon~ios.png` (both from `final/ios/AppIcon-1024.png`), `docs/assets/icon.png`, and the
   `branding/` folder (`logo.svg` plus the 1024, store and preview PNGs). To change the icon, edit the
   masters in `final/svg/`, re-export, and copy the files to those places again.
3. **Stripe ratio.** The logo stripe is 14 : 6 (width : gap); the in-app band is 12 : 6. They are
   visually the same family but not identical; keep them separate rather than aligning them.
4. **Wordmark kerning.** Spacing was set by eye with uniform tracking; font kerning pairs were not
   applied. Check it at very large sizes (signage).
5. **No PDF / EPS / AI.** Only SVG and PNG masters were produced. Print vendors that need PDF can
   export from the SVG in any vector editor.
