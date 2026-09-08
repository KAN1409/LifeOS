# Log4brains upstream pin

Repository: https://github.com/thomvaill/log4brains
Pinned commit: `17e32021a8c5130386f17e921d4efa6da7709a66`
License: Apache-2.0

LifeOS use:
- decision-record semantics: Context -> Decision -> Consequences
- decision status lifecycle: proposed / accepted / deprecated / superseded
- immutable historical record with explicit supersession
- chronological decision timeline

Integration rule: Log4brains is the provenance/model donor for Decision Memory. Graphiti is currently the runtime temporal graph used to surface decision facts in the Android app; LifeOS glue must preserve the decision lifecycle and provenance semantics rather than inventing a separate decision history model.
