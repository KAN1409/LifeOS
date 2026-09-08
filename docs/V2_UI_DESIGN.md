# LifeOS V2 UI Design Direction

LifeOS V2 should use a visual language strongly inspired by the GitHub Mobile app while remaining distinctly LifeOS in branding, icons, naming, and content.

## Core principles

- Dense but calm information hierarchy.
- Minimal decoration; content and state are primary.
- Strong separation using spacing, dividers, subtle surfaces, and restrained elevation rather than oversized cards.
- Excellent light and dark themes.
- Compact typography with clear title/body/metadata hierarchy.
- List-first navigation for situations, priorities, people, events, and actions.
- Native Android interaction patterns and predictable back/navigation behavior.
- Clear status indicators, badges, chips, timestamps, and secondary metadata.
- High information density without visual clutter.

## Navigation

Primary destinations should follow the clarity of GitHub Mobile rather than dashboard-heavy consumer UI.

Suggested LifeOS destinations:
- Home / Attention
- Situations
- Timeline
- People
- Actions
- Search

Use a compact bottom navigation or equivalent native structure for the highest-frequency destinations. Deeper objects open as focused detail screens with a clear app bar and contextual actions.

## Screen language

### Home / Attention
Use a feed/list structure rather than a grid of cards. Priority rows should contain:
- concise title;
- why it matters;
- source/evidence metadata;
- state badge;
- timestamp/age;
- optional one-line recommended next action.

### Situation detail
Use a repository/issue-style detail hierarchy:
- title and state at top;
- compact metadata;
- participants/entities;
- current interpretation;
- timeline/evidence;
- open loops;
- suggested actions;
- related situations.

### Timeline
Use compact event rows with source icon, event summary, timestamp, source app, and provenance. Avoid oversized message bubbles unless the source content itself requires them.

### Actions / approvals
Approval requests should resemble a clean review queue: action summary, target, reason, evidence, risk level, and explicit Approve / Reject controls. Never make approval ambiguous.

## Visual tokens

- Neutral backgrounds and surfaces.
- Subtle borders/dividers.
- Restrained corner radii; avoid excessive rounded-card styling.
- Accent color reserved for selected state, links, primary actions, and meaningful status.
- Use semantic colors only for status/risk/success/warning/error.
- Icons should be simple line/filled system-style icons, not GitHub trademarks.
- Avoid gradients, glassmorphism, neon effects, decorative illustrations, and large empty hero areas.

## Branding boundary

Mimic the usability, information density, hierarchy, spacing discipline, navigation clarity, and light/dark behavior of GitHub Mobile. Do not copy GitHub logos, Octicons in a trademark-confusing way, proprietary illustrations, screenshots, exact branded color combinations, or text/content.

## Product rule

When a new LifeOS feature is designed, default to the question: “How would a compact developer-grade mobile product present this information clearly?” rather than adding another dashboard card.
