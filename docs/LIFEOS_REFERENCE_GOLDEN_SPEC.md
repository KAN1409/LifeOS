# LifeOS Golden Reference UI Specification

Status: **LOCKED GOLDEN REFERENCE**

Scope: `Now`, `Timeline`, `Search`, `Ask LifeOS`, shared navigation, typography, iconography, semantic presentation.

Reference raster: **756 × 1536 px** portrait concept screens. Implementation normalizes to an approximately **378 dp-wide** Android phone canvas, then adapts through dp/sp and system insets.

This document is a measured/inferred reconstruction from raster artwork rather than a Figma export. The values below are therefore the implementation contract unless Kareem explicitly approves a change.

## Non-negotiable principles

1. The reference images are the visual source of truth. APK screenshots are implementation output, never the benchmark.
2. The UI is calm, dense, premium, dark, information-first and deliberately non-Material-looking.
3. The normal UI never exposes internal engine vocabulary such as `DEADLINE`, `WAITING_ON_USER`, queue state names, model names, Graphiti, Teya or SecondBrain.
4. Raw notification bodies are evidence, not presentation. They belong behind Evidence/Original source.
5. One real-world event appears once in its most useful semantic form. Duplicate notification fragments, typing indicators, reactions and repeated generic social activity collapse.
6. Canonical name spelling is **Kareem**, never Karim.
7. User-facing prose addresses Kareem as **you / your**, never “the user”.
8. Blue is a sparse interaction/accent color, not a decorative wash.
9. Cards are mainly for high-value Now items and Ask prompts. Timeline and Search are primarily compact flat rows.
10. Proper vector icons are mandatory. Unicode glyph stand-ins are prohibited in the polished UI.
11. Real source-app icons are preferred when available. Generic fallbacks must communicate category.
12. All screens respect status bar, cutout, gesture and navigation insets.

## Canvas and grid

- Reference: 756 × 1536 px.
- Logical target: ~378 × 768 dp before system-bar adaptation.
- Primary screen gutter: **16 dp**.
- Icon-to-copy gap: **10–12 dp**.
- Major card gap: **7–8 dp**.
- Chip gap: **6–8 dp**.
- Major section separation: **16–20 dp**.
- Standard Now card radius: **15 dp**.
- Ask prompt radius: **14 dp**.
- Search/composer radius: **26 dp**.
- Filter-chip radius: **20 dp**.
- Icon tile radius: **11 dp**.
- Avatar: **40 dp circular**.
- Send: **50 dp circular**.
- Standard border: **1 dp**.
- Standard cards use no drop shadow; depth comes from subtle fill + border.

## Color contract

| Token | Value | Usage |
|---|---:|---|
| `bg` | `#070B10` | page background |
| `surface` | `#111820` | cards and bottom bar |
| `surfaceRaised` | `#1C2530` | search field, composer, inactive controls |
| `border` | `#2A343F` | hairlines and card borders |
| `textPrimary` | `#EEF2F6` | titles and important values |
| `textSecondary` | `#919AA6` | summaries, dates, metadata |
| `textTertiary` | `#697482` | low-priority metadata, inactive icons |
| `blue` | `#3288FF` | active tab, primary actions, links, waiting-on-you |
| `red` | `#F04C59` | overdue/urgent/due |
| `green` | `#43BE68` | ready/positive/commitment |
| `purple` | `#8A60FF` | people/events, orb secondary arc |
| `pink` | `#EF4C8F` | decisions |
| `amber` | `#E8A52B` | school/files/warnings |

Normal card fill remains dark even when urgent. Status colors belong to text/icon accents rather than full-card backgrounds.

## Typography

The reference resembles SF Pro / Inter / neutral neo-grotesk. Android should use `sans-serif` with explicit weights.

| Role | Size | Weight |
|---|---:|---:|
| Screen title | 22–24 sp | 700 |
| Greeting / Ask hero | 22–24 sp | 700 |
| Section heading | 16–17 sp | 700 |
| Attention/event title | 13.5–15 sp | 600–700 |
| Body/summary | 11.5–13 sp | 400 |
| Status/metadata | 10.5–12 sp | 500 |
| Chip | 10–11 sp | 500–600 |
| Stat label | 9–10 sp | 500 |
| Bottom-nav label | 9–10 sp | active 600, inactive 400–500 |

Leading stays tight (~1.05–1.12×). Android default Button capitalization/elevation/padding are not part of the design.

## Shared vector iconography

Icons are outline-first with rounded caps and joins, roughly 1.7–2 dp stroke.

Bottom navigation:
- Now: outline home.
- Timeline: three horizontal rows with left markers.
- Search: magnifying glass.
- Ask: rounded speech bubble with subtle sparkle/dot.
- Visible icon size: **21–23 dp**.
- Label gap below icon: **3–5 dp**.

Header utilities:
- Search: 21–23 dp magnifying glass.
- Filter: compact filter/sliders motif.
- History: circular-arrow/clock motif.
- More: vertical three dots.
- Visible glyph ~22 dp, touch target ~42 dp, no visible button container.

Search/composer:
- Proper microphone icon, ~20 dp.
- Send uses paper-plane icon centered in blue circle.
- Chevron is thin and subtle.

Category tiles (~34–42 dp):
- commitment: green document/check.
- decision: pink diamond.
- people: violet people.
- files: amber document.
- places: blue map pin.
- events: purple calendar.
- school payment: amber graduation cap.
- car care: blue car.

## Shared bottom navigation

- Four equal destinations: `Now`, `Timeline`, `Search`, `Ask`.
- Surface slightly lighter than page background.
- 1 dp top divider.
- Content height **60 dp** plus system inset.
- Active icon + label blue.
- Inactive icons/text tertiary gray.
- No active pill or filled selected-tab background.
- No visible transition animation between top-level tabs.

# NOW

## Header

- Left: blue LifeOS ring ~20 dp, 8 dp gap, `LifeOS` ~22 sp bold.
- Right: circular `K` avatar, ~40 dp, dark raised fill + subtle border.
- No gear in the main header.

## Processing status

The reference has no permanent `75 ready · 0 processing` line.

- Idle + zero pending: hide status.
- Processing: show a subtle temporary line such as `2 processing`.
- Pull-to-refresh may reveal processing state.
- Failure/retry appears only while relevant.

## Greeting

- Date: 11–12 sp secondary.
- Greeting: `Good morning/afternoon/evening, Kareem.` 22–23 sp bold.
- Subtitle: `Here's what matters today.` 12–13 sp secondary.
- Greeting-to-stat gap ~13 dp.

## Stat row

Three equal cards, 58–64 dp tall, 6–8 dp gaps, ~14 dp radius.

Each includes a tiny semantic vector icon, 17–19 sp value and 9–10 sp label.

- Need attention: red.
- Ready actions: blue.
- Upcoming: neutral/gray.

## Needs Attention

Section heading: 16–17 sp bold; `See all` on right in ~11 sp blue.

Attention card:
- radius 15–16 dp.
- 1 dp border.
- typical height 76–94 dp.
- 12–14 dp inner padding.
- semantic/source icon 40–44 dp.
- copy gap 10–12 dp.
- title 13.5–15 sp semibold/bold.
- status/date 10.5–11.5 sp semantic color.
- concise summary 11–12 sp secondary, max two lines.
- thin chevron at far right.

Golden examples:

**School fee payment**
`Tomorrow · 5 Sep`
`First installment is still unpaid.`

**GLIO Car Care**
`Waiting on you`
`They need your car details to confirm the booking.`

**Revised quotation**
`Overdue · 1 day`
`Ahmed is waiting for the revised quotation.`

Forbidden on normal cards: raw enums, model confidence, generic `info`, or multiline raw notification bodies.

## Approvals

`Ready for your approval` uses same section-header pattern.

Approval card includes source/action icon, concise title, `Draft reply ready`, draft copy, then `Edit/Cancel` and blue `Approve` controls around 42–46 dp high.

## Situation de-duplication

A Situation must not appear on Now if it is merely the same evidence already shown in Needs Attention. A Situation is allowed only when it adds higher-level context across multiple distinct events.

# TIMELINE

Purpose: **semantic life timeline**, not notification history.

## Header
- `Timeline`: 22–23 sp bold.
- Right: Search + Filter vector icons, each ~22 dp inside ~42 dp touch targets.

## Chips
`All`, `Messages`, `Emails`, `Calls`, `Events`.
- height 34–38 dp.
- radius 18–20 dp.
- active blue/white.
- inactive raised dark + subtle border.

## Day header
`Today · Thu, 4 Sep 2026` / `Yesterday · Wed, 3 Sep 2026`, ~12.5 sp semibold secondary.

## Rail anatomy
Each row has four zones:
1. time column ~49 dp, right-aligned 10–11 sp.
2. rail ~27 dp, continuous 1 dp line + 7 dp semantic dot.
3. source app icon 38–42 dp.
4. semantic text block.

Row target: **70–82 dp**, nominal 76 dp.

Text:
- title 13–14 sp bold, one line.
- summary 11–12 sp secondary, one line.
- optional thin chevron/paperclip at far right.

Suppress/merge:
- typing indicators.
- reply/reaction/like activity.
- routine follows/story activity.
- promotional pushes with no obligation.
- repeated generic voice-note fragments.
- duplicated notification/screen-capture forms of one event.

Near-identical semantic title + summary + source in a short window coalesce. Never surface `User received…`; use direct `You…` copy or a person/source title plus meaningful content.

# SEARCH

## Header
`Search`, 22–23 sp bold.

## Search field
- 16 dp side margins.
- 50–54 dp height.
- ~26 dp radius.
- raised surface.
- proper 20 dp search icon left.
- `Search your life…` 13 sp secondary.
- proper 20 dp microphone icon right.

## Chips
`All`, `People`, `Files`, `Places`, `Decisions`, same geometry as Timeline chips.

## Recent searches
Only real persisted history.

Header: `Recent searches` left, `Clear` blue right.
Rows are flat, ~42–46 dp, with history icon, query and chevron. No full-card background.

## Suggested
Flat two-line rows with 36 dp tinted icon tile, 13 sp semibold title, ~11 sp muted subtitle, and chevron.

Mappings:
- Open commitments — green.
- Decisions — pink.
- People — violet.
- Files — amber.
- Places — blue.
- Events — purple.

Search result mode stays similarly compact; results are not oversized cards and should use semantic titles/summaries rather than raw notification payloads.

# ASK LIFEOS

## Header
`Ask LifeOS`, 22–23 sp bold.
Right: History + vertical More vector icons. No settings gear.

## Orb
The only strong decorative object in the product.
- visible ring diameter ~100–115 dp.
- ring stroke 2.5–3 dp.
- blue primary arc with violet transition.
- center stays dark/transparent.
- soft glow extends ~18–28 dp beyond ring.
- container height ~150–165 dp.

## Hero
`How can I help you?` 22–24 sp bold centered.
`Ask. Plan. Decide. Do.` 12–13 sp secondary centered.

## Prompt cards
Five prompts, 14 dp radius, subtle border, ~14 dp horizontal padding, minimum 56 dp height, ~8 dp gaps, 12–13 sp copy and thin chevron.

Reference prompts:
- What needs my attention today?
- Summarize my conversations with Ahmed this week.
- Find the latest gypsum board price and compare it.
- What did I decide about the car?
- Draft a reply to GLIO with my car details.

## Composer
Directly above bottom nav.
- input pill 50–54 dp high, ~26 dp radius.
- `Ask anything…` secondary.
- proper microphone icon inside at right.
- send is a true ~50 dp blue circle with white paper-plane vector and subtle elevation/glow.

# Semantic presentation contract

1. Presentation meaning must be bound to the **exact evidence item**, not the latest meaning for an entire conversation/thread.
2. Background semantic summaries are concise natural English suitable for direct display in the English LifeOS UI.
3. The Brain may use context from earlier messages, but only the target evidence may create target meaning/state.
4. Search, Timeline and Now consume semantic presentation, not raw capture bodies.
5. If no trustworthy semantic summary exists, prefer a safe source/category label over dumping long raw evidence.
6. `Kareem` and direct `you/your` wording are enforced globally.
7. Visual fidelity regressions against this document are bugs, not alternative design choices.

# Acceptance checklist for every UI release

- Now: no idle processing line; circular avatar; correct Kareem spelling; compact stats; semantic cards; no duplicate Situations.
- Timeline: rail geometry present; real app icons; no social-noise flood; no generic repeated voice-note events; no raw `User…` wording.
- Search: vector search/mic/history/category icons; real recent history; flat Suggested and result rows.
- Ask: history/more vectors; glowing blue-violet orb; compact prompt cards; mic in composer; circular paper-plane send.
- Bottom nav: true vectors; no Unicode glyph stand-ins; active blue, inactive tertiary; 60 dp bar + insets.
- No raw model enums or donor names in user-facing primary screens.
