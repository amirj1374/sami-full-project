# SAMI frontend checklist

## Discovery and reuse

- Guidance, branch, worktree and package manager verified
- Comparable feature inspected end to end
- Components and UI-kit wrappers identified
- Composables, utilities and formatters identified
- Theme tokens, global styles and breakpoints identified
- API, store, route, permission and validation patterns identified
- Reuse plan stated before edits

## Implementation

- Vue Composition API and strict types follow project conventions
- Backend lifecycle, configuration, flags and permissions remain authoritative
- API calls use established typed clients
- Local versus shared state is scoped appropriately
- Loading, error, retry, empty, disabled and forbidden states exist
- Duplicate submissions and destructive actions are handled
- Layout is responsive and translation-safe
- Keyboard, focus, labels, semantics and contrast are preserved
- Timers, listeners, requests and object URLs are cleaned up

## Localization and RTL

- No hardcoded user-facing text
- English and Persian keys added together
- Key sets and interpolation variables match
- Locale changes update reactive text
- Directional layout, icons and ordering work in RTL and LTR

## Verification

- Dependency install used only when needed
- Type-check passes
- Configured lint and tests pass
- Production build passes
- Desktop and mobile browser flows verified when available
- Console and network errors checked
- Unavailable checks and backend assumptions are reported
