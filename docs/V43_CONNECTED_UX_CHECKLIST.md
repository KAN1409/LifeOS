# LifeOS v43 — Connected UX implementation checklist

This checklist is the acceptance contract for the “simple surface, powerful underneath” proposal. A box is checked only when the implementation is present in the v43 source tree. Build/sign/device verification remain separate gates at the bottom.

## A. Top-level information architecture
- [x] Keep exactly four primary destinations: Now / Timeline / Search / Ask.
- [x] Do not add People, Files, Places, Decisions, Projects, Events, Conversations, or Commitments as extra bottom-nav tabs.
- [x] Keep the bottom navigation persistent and visually consistent.
- [x] Preserve the existing LifeOS package/update identity.

## B. Now — focused command center
- [x] Keep the personal greeting and “what matters now” framing.
- [x] Show a real LifeOS status ring in the top bar.
- [x] Status ring has grounded states: idle/ready, analyzing/background work, needs attention/setup.
- [x] Tapping the status ring opens human-readable LifeOS health/status.
- [x] Need attention count is grounded in durable attention/open-loop evidence and deduplicated by evidence.
- [x] Ready actions count comes from the persistent action queue.
- [x] Upcoming count comes from real dated open-loop state.
- [x] All three summary tiles are interactive gateways, not decorative counters.
- [x] “Priority for today” exposes only a small ranked/grounded set.
- [x] “Active situations” uses SituationEngine, not raw notification rows.
- [x] Situation previews suppress raw phone-number/message leakage when the semantic summary is unsafe.
- [x] “Suggestions for you” shows real pending actions only.
- [x] Suggested actions retain explicit approval before execution.

## C. Timeline — everything in context
- [x] Keep the semantic timeline rail and day grouping.
- [x] Keep app/source icons and contextual summaries.
- [x] Keep event collapse/fingerprinting so one real episode is not shown repeatedly.
- [x] Keep semantic noise filtering before presentation.
- [x] All / Messages / Emails / Calls / Events filters remain available.
- [x] The filter icon is functional and toggles the filter strip.
- [x] Search icon is functional.
- [x] Conversation-like rows open conversation context; other rows enter the common entity/evidence flow.

## D. Search — capability hub
- [x] Search remains one of the four primary entrances.
- [x] Search bar and voice search remain available.
- [x] Search category chips remain available.
- [x] Landing screen is reorganized around “Browse LifeOS”.
- [x] Browse LifeOS uses a compact two-column/bento capability grid instead of a long feature list.
- [x] People capability is visible.
- [x] Conversations capability is visible.
- [x] Files capability is visible.
- [x] Decisions capability is visible.
- [x] Places capability is visible.
- [x] Events capability is visible.
- [x] Projects capability is visible.
- [x] Commitments capability is visible.
- [x] Capability counts are calculated from grounded local LifeOS data; no decorative/fake numbers.
- [x] Capability secondary status text is derived from real captured state.
- [x] Each capability tile opens a real reusable capability browser.
- [x] Recent searches remain visible below the capability grid when history exists.
- [x] Search results deep-link into Person, Conversation, or common entity details.

## E. Ask — contextual reasoning surface
- [x] Keep the LifeOS orb and conversational composer.
- [x] Remove the old hard-coded five-prompt gallery as the source of suggestions.
- [x] Generate suggestions dynamically from current grounded LifeOS state.
- [x] Attention can generate a contextual Ask suggestion.
- [x] A recent person/conversation can generate a contextual Ask suggestion.
- [x] A grounded project can generate a contextual Ask suggestion.
- [x] A remembered decision can generate a contextual Ask suggestion.
- [x] A pending approved-action candidate can generate a contextual Ask suggestion.
- [x] Fill unused suggestion slots from real capabilities only.
- [x] Contextual prompt cards show capability-specific icons and explanatory subtext.
- [x] Ask can be opened from deep detail with a prefilled “Ask about this” question.
- [x] Voice input and recent-question history remain available.

## F. Deep capability architecture
- [x] Add a single LifeOsCapabilityRegistry as the source of capability labels/icons/counts/queries.
- [x] Add a reusable CapabilityActivity instead of creating eight unrelated browser screens.
- [x] Capability browser uses real LifeIntelligenceEngine/LifeDb results.
- [x] Two-path discovery exists: visible capability browsing plus contextual cross-links.
- [x] Contextual routes can lead from one entity to related capabilities.

## G. Consistent detail architecture
- [x] Generic entity detail follows: Summary → What matters → Related → Evidence → Actions.
- [x] Generic entity detail includes “Ask about this”.
- [x] Generic entity detail includes “Search related”.
- [x] Person detail uses the same LifeOS visual language.
- [x] Person detail includes Summary.
- [x] Person detail includes grounded quick stats / What matters.
- [x] Person detail includes related conversation/search paths.
- [x] Person detail includes evidence rows.
- [x] Person detail includes “Ask about <person>”.
- [x] Conversation detail uses the same LifeOS visual language.
- [x] Conversation detail includes Summary / What matters / Related / Evidence / Actions.
- [x] Situation detail uses the same LifeOS visual language.
- [x] Situation detail includes grounded attention/action/evidence counts.
- [x] Situation detail suppresses unsafe raw summary leakage.
- [x] Suggested-action detail uses the same LifeOS visual language.
- [x] Suggested-action detail exposes rationale/evidence before approval.
- [x] Suggested-action approval executes only after explicit user approval.
- [x] Evidence detail uses the same LifeOS visual language and remains the provenance layer.
- [x] Evidence detail keeps “open original source” and explicit “mark handled” behavior.
- [x] Feed-section drill-down pages use the same LifeOS visual language.
- [x] Action Center uses the same LifeOS visual language.

## H. System status and trust
- [x] Status/health is human-readable instead of exposing donor/runtime plumbing on primary screens.
- [x] LifeOS health shows Capture, Background intelligence, Memory, Processing, and Attention.
- [x] Missing capture access is surfaced as an actionable state.
- [x] Model preparation/failure state is visible without blocking the normal UI.
- [x] Notification and screen-context settings remain reachable.
- [x] Local data erase remains explicit and confirmed.

## I. Visual continuity / no drift
- [x] Preserve v42 dark palette, typography scale, borders, icon weight, and optical clarity baseline.
- [x] Preserve the four-tab bottom-nav visual language.
- [x] Do not introduce a hamburger feature dump.
- [x] Do not add fake feature cards or dead controls.
- [x] Keep secondary/tertiary text readable on dark surfaces.
- [x] Keep compact, information-rich layout instead of giant decorative panels.

## J. Release safety gates
- [x] Version bumped to v43 / versionCode 43.
- [x] No PR merge performed.
- [ ] Android unit tests pass in CI.
- [ ] Release APK build passes in CI.
- [ ] CI artifact downloaded and inspected.
- [ ] Native libraries verified for 16KB alignment.
- [ ] APK signed with the permanent LifeOS signer.
- [ ] APK Signature Scheme v2 verified.
- [ ] APK Signature Scheme v3 verified.
- [ ] Package/version verified as an in-place update.
- [ ] Final SHA-256 recorded.
- [ ] Device screenshots verified for Now / Timeline / Search / Ask.
- [ ] Device drill-down verified for at least Person / Capability / Situation / Action / Evidence.

## Definition of done
v43 is not considered complete until every Release safety gate above is checked. Source implementation being present is necessary but not sufficient; CI, signing, update identity and device proof remain mandatory.
