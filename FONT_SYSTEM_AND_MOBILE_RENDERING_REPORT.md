# SAMI Font System and Mobile Rendering Report

## Outcome

The frontend now has one production-safe, self-hosted typography owner for Persian RTL and English LTR. Vazirmatn Variable is applied through the document, Vuetify application and overlay roots, native form controls, menus, dialogs, snackbars, tooltips and SVG/chart text. No backend, API, permission, localization content or business behavior changed.

## Root cause

The correct self-hosted font package was present and imported, but the application only assigned the family to `body` and `.v-application`. The boot screen referenced an unavailable `Inter` font, native controls and teleported Vuetify overlays were not covered explicitly, Persian controls retained component-specific Latin tracking, and the PWA cache namespace had not changed since the original shell cache. On mobile and installed PWA launches those gaps made system-font fallback visible and could preserve an older CSS shell.

## Canonical font inventory

- Family: `Vazirmatn Variable` from `@fontsource-variable/vazirmatn` 5.2.8 (OFL-1.1).
- Style: normal.
- Weight range: 100–900 variable; the browser selects real variable instances rather than mapping one static file to artificial weights.
- Persian/Arabic WOFF2 subset: 46,308 bytes.
- Latin WOFF2 subset: 34,524 bytes.
- Latin Extended WOFF2 subset: 21,860 bytes.
- Total application font payload: 102,692 bytes (about 100.3 KiB), split by correct `unicode-range` declarations.
- `font-display: swap` is supplied by Fontsource.
- No remote font CDN, proprietary asset or duplicate application font was introduced. Material Design Icons remains a separate icon font and is not part of application typography.

## Implementation

- `sami-frontend/src/styles/global.css`
  - Defines locale-aware Persian and English family stacks and minimal display, heading, body, label and caption tokens.
  - Applies typography to `html`, `body`, `#app`, `.v-application`, `.v-overlay-container`, native controls, Vuetify overlay surfaces and SVG/chart text.
  - Uses `font-synthesis: none` to avoid faux styles and keeps direction-sensitive tracking: normal Persian tracking and restrained English heading tracking.
  - Gives Persian body copy a mobile-friendly line height without changing component layout.
- `sami-frontend/index.html`
  - Removes the nonexistent boot-screen `Inter` reference and uses a platform-safe startup fallback until the bundled CSS loads.
- `sami-frontend/public/service-worker.js`
  - Advances the shell cache namespace to `sami-shell-v2`; activation removes obsolete `sami-*` caches while Vite-hashed fonts remain safely immutable/offline-cacheable.
- `sami-frontend/tests/font-assets-smoke.mjs` and `package.json`
  - Adds `npm run test:font-assets`, which parses emitted CSS and fails if a referenced built font is missing/empty or if the Persian/Arabic Vazirmatn subset is absent.
- `.agents/SAMI_PROJECT_CONTEXT.md`
  - Records the canonical shared typography owner and its build validation command.

## Vuetify and locale integration

Vuetify continues to own its component structure and theme. The font is inherited at its application and teleported overlay boundaries, so fields, buttons, data tables, dialogs, menus, snackbars and tooltips use the same family without scattered component overrides. Native inputs explicitly use `font: inherit`. Persian uses `Vazirmatn Variable, Vazirmatn, Tahoma, Arial, sans-serif`; English uses `Vazirmatn Variable, Segoe UI, Roboto, Arial, sans-serif`.

Browser-computed styles confirmed `Vazirmatn Variable` on the body, Vuetify root, input, button and heading in both locales. Persian `lang=fa`/`dir=rtl`, English `lang=en`/`dir=ltr`, and both font checks resolved from the loaded FontFaceSet. Persian headings and controls have zero forced letter spacing.

## Mobile verification

The production nginx build was tested in Chromium responsive view at 360, 375, 390, 412 and 430 CSS pixels in Persian RTL. Each width retained the Vazirmatn stack, a 26.25 px body line height, zero document/body horizontal overflow, zero clipped sampled controls and a readable responsive heading line height. The login form, labels, placeholder text, mixed brand copy and actions remained legible at 360 px.

This verifies the real production bundle under Chromium's Android-equivalent rendering path. Physical Android installed-PWA and iOS Safari/home-screen testing remain device acceptance checks because those runtimes are not available in the local toolchain.

## Production, Docker and nginx verification

- `npm run build` emitted all three hashed Vazirmatn WOFF2 assets and CSS references only hashed `/assets/...` URLs.
- `npm run test:font-assets` resolved every CSS font reference to a present, non-empty file.
- Linux image: `sami-frontend:font-system-test`, successfully built and inspected as `linux/amd64`.
- The font files are present in `/usr/share/nginx/html/assets` inside the runtime image.
- A direct request to the Persian/Arabic WOFF2 returned HTTP 200, `Content-Type: font/woff2`, one-year expiry and `public, immutable` caching.
- `/service-worker.js` returned HTTP 200 with `no-cache, no-store, must-revalidate` and contains `sami-shell-v2`.
- HTML points to hashed CSS; changed CSS receives a new filename, and service-worker activation removes the previous shell cache. Existing long-lived font URLs remain safe because their content hashes identify their bytes.

The frontend nginx image resolves the `backend` hostname at startup as designed by production Compose. Standalone verification attached the image to an existing SAMI test network providing that DNS name; deployment configuration was not changed.

## Validation executed

- `npm test`: 9/9 passed, including exact English/Persian localization parity.
- `npm run type-check`: passed.
- `npm run build`: passed (1,021 modules transformed).
- `npm run test:font-assets`: passed; 3 Vazirmatn assets verified.
- Browser desktop English and Persian computed-style/font checks: passed.
- Browser responsive checks at 360/375/390/412/430 px: passed.
- Docker Buildx `linux/amd64` production image build: passed.
- Runtime nginx root/service-worker/font HTTP checks: passed.
- `git diff --check`: passed.

## Real-phone verification

1. Deploy a build containing the final development revision to the test environment.
2. On Android Chrome or iOS Safari, open the site in a private tab first to separate the new build from old storage.
3. Select Persian and verify the login page, one data table, one dialog/menu, a text field placeholder and a bold heading. In remote developer tools, confirm the computed family begins with `Vazirmatn Variable` and the Arabic WOFF2 request returns 200.
4. Repeat in English and confirm LTR direction, readable Latin spacing and the same primary family.
5. Test portrait widths representative of 360–430 CSS pixels, browser zoom/text-size defaults and one increased accessibility text-size setting.
6. Install/add the application to the home screen, launch it independently and repeat the Persian/English checks online and once offline after a successful online load.

## Refreshing an already-installed PWA

1. Launch the installed app while online and leave it open long enough for the no-cache service-worker check to complete.
2. Close every window of the installed app, then reopen it. The new worker activates and removes the previous `sami-shell-v1` cache.
3. If the device still shows the old shell, remove the installed app, clear site data for the SAMI origin in browser settings, revisit the site online, verify the new typography, and install it again.
4. Do not clear unrelated browser data; target only the SAMI origin.

## Remaining limitations

- Physical Android Chrome/PWA and iOS Safari/home-screen runs were not available locally; responsive Chromium and production HTTP behavior were verified instead.
- Font swapping can cause a small first-render metric change on an uncached device. `font-display: swap`, unicode-range subsetting and the 100.3 KiB total payload bound that cost; no render-blocking preload was added.
- The existing npm install reports one high-severity dependency advisory. It predates and is unrelated to typography, so dependencies were not upgraded outside scope.

## Git integration

- Feature branch: `codex/fix-mobile-font-system`.
- Development integration commit: `PENDING_INTEGRATION_SHA`.
