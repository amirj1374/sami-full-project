# Frontend architecture

The frontend is a Vue 3 Composition API SPA using strict TypeScript,
`<script setup>`, Vuetify, Pinia, Vue Router, Axios, Vue I18n, VeeValidate, Zod,
VueUse, and optional MSW.

## Structure and flow

- Views compose feature behavior and shared components.
- `src/api` owns HTTP calls; views should not create independent Axios clients.
- `src/types` and `src/schemas` mirror backend contracts and validation.
- Pinia stores own authentication and menu state; page-local state remains local.
- Router metadata declares authentication and presentation permissions.
- `src/locales/en.json` and `fa.json` must evolve together.
- Vuetify direction/theme plus localized content must work in RTL and LTR.

## Authentication and errors

The shared Axios instance adds bearer tokens and handles single-flight refresh.
Use the shared error composable/model rather than displaying raw server
responses. Never treat UI permission checks as authorization.

## Adding a page safely

1. Verify the backend endpoint and DTO.
2. Add/update TypeScript types and API client.
3. Add schemas and localized validation messages.
4. Build reusable components before duplicating tables/forms.
5. Add a lazy route with auth/permission metadata.
6. Add both locale entries and verify RTL/LTR.
7. Update MSW only when it remains contract-aligned.
8. Run type-check and production build.

No frontend lint or automated test framework is currently configured.
