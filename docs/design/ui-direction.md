# UI direction

Minimal, calm, easy to understand at a glance. Inspired by the monochrome, dot-matrix look of
Nothing OS, without copying its brand.

## Principles

1. **One screen, one job.** Home answers "what's happening right now, and why?"; every
   setting is one tap away, never buried.
2. **Monochrome first.** Black and white do the work; the single red accent marks only the
   active choice or the one thing that needs attention.
3. **Say it in words.** Status lines explain decisions in plain sentences ("Teams quiet until
   Mon 09:00 · 4 held"), not icons alone.
4. **Space over chrome.** Outlined panels on a plain background, generous padding, no
   gradients or shadows.

## Tokens (in `core:designsystem`)

| Token | Value | Use |
|---|---|---|
| Black / White | #000000 / #FFFFFF | Background and text (dark and light themes) |
| Ink | #111111 | Dark-theme raised surfaces |
| Paper | #F4F4F2 | Light-theme background |
| Gray / GrayDark / GrayLight | #8A8A8A / #2A2A2A / #D6D6D3 | Secondary text, outlines |
| Accent | #E5322D | Selected state, warnings; used sparingly |

Type: a dot-matrix display face for headings and numbers, monospace uppercase labels, a
clean sans for body text. The display font must be openly licensed (OFL), e.g. Doto from
Google Fonts. Until it's added, the display style falls back to monospace.

## Components

`UtPanel` (outlined container), `UtLabel` (small monospace label), `UtSegmentedChoice`
(profile switch and other single choices). New components are added to the design system
first, then used in screens.

## Not allowed

Nothing OS's own typefaces (NType, NDot) or brand assets, which aren't licensed for other
apps; the words "Nothing" or "Xiaomi" in the app or listing.

## Accessibility

Text contrast at least WCAG AA in both themes; every interactive element has a TalkBack
label and selection state; touch targets at least 48 dp; layouts work at 200% font size.
