# Form Refactor Deployment Verification Report

Date: 2026-08-03  
Repository: `sami-full-project`  
Authoritative branch: `development`

## Assessment

**MERGED_BUT_NOT_INCLUDED_IN_IMAGE**

The form refactor is committed directly on `development` as
`08c28d151870ab0f58e521ca617dbf54480ddef8` (`fix(ui): improve mobile form
spacing`). It is contained by the local `development` branch. The latest
authoritative deployment manifest and the locally retained deployment image,
however, identify frontend revision
`3e83ff6904e7c3fb50164ec56d59b45e87e7e89c`. Git proves that the form-refactor
commit is not an ancestor of that revision.

At the time of this audit local `development` was five commits ahead of
`origin/development`; the form commit had therefore not reached the remote
integration branch used by the recorded image. The reported lack of a deployed
visual difference is consistent with the deployed artifact evidence and is not
caused by the refactor CSS being too subtle in that artifact: the artifact does
not contain it.

## Git evidence

- Branch containing the refactor: `development`
- Refactor commit: `08c28d151870ab0f58e521ca617dbf54480ddef8`
- Parent: `5db14cd251fe9780b8451e26e8ab6ac8c4f50aa4`
- Commit time: `2026-08-02T18:13:46+03:30`
- Commit state: committed, cleanly contained by local `development`
- Remote state at audit start: `development...origin/development [ahead 5]`
- Separate feature branch/merge: none; the commit was made directly on
  `development`

Changed files in the commit:

- `sami-frontend/src/components/AppFormSection.vue`
- `sami-frontend/src/plugins/vuetify.ts`
- `sami-frontend/src/styles/global.css`
- `sami-frontend/tests/release-contracts.test.mjs`
- `docs/screenshots/form-ux-before-mobile.png`
- `docs/screenshots/form-ux-after-mobile.png`

Diff size: 126 insertions and 14 deletions across four text files, plus two
screenshots.

## Measurable before and after values

| Behavior | Before (`5db14cd`) | After (`08c28d1`) |
|---|---|---|
| Input density | Vuetify `comfortable` | Vuetify `compact` |
| Input minimum height, desktop | `48px` (`--app-control-height`) | `44px` (`--app-form-control-height`) |
| Input minimum height, coarse/mobile pointer | `52px` | `48px` |
| Label behavior | ordinary Vuetify floating labels | controls are forced active; floating labels are fully opaque and weight 600 |
| Label gap token | none | `7px` declared; labels remain Vuetify outline labels rather than separate external labels |
| Field-group vertical rhythm | inherited Vuetify row gutters (normally 24px between full rows) | `16px` desktop, `14px` mobile; direct row-column block padding is removed |
| Validation top spacing | `5px`, with Vuetify details minimum height | `4px`, details/messages minimum height `0`; empty details collapse |
| Section-to-section spacing | `16px` | `24px` desktop, `20px` mobile |
| Dialog header minimum height | `64px` | `56px` |
| Dialog body padding | framework/default desktop padding; mobile horizontal padding `16px` | explicit `24px` desktop and `16px` mobile |
| Dialog footer minimum height | `64px` | `60px` |
| Dialog footer padding | mobile horizontal `16px`; otherwise framework default | `16px 24px` desktop and `16px` mobile, with safe-area bottom maximum |
| Mobile sticky actions | already sticky with safe-area padding | retained; card becomes a bounded flex column, body independently scrolls, footer is non-shrinking, and non-icon actions share width |
| Form-section padding | `16px 20px 20px` | `16px` desktop, `12px` mobile |

The declared `--app-form-label-gap` is not consumed by a separate-label layout;
it is a design token reserved by the commit while existing Vuetify outlined
labels continue to sit in the outline. This is a verified limitation of the
implementation, but it is not the cause of the deployed result because the
deployed image predates the entire commit.

## Image and asset evidence

Recorded deployment manifest (`deployment-artifacts/release-manifest.json`):

- Branch: `development`
- Frontend revision: `3e83ff6904e7c3fb50164ec56d59b45e87e7e89c`
- Build timestamp: `2026-08-01T21:59:52Z`
- Frontend image ID:
  `sha256:8d1ed3350efeb2ce2c4ac65b18a0457568e0b13d3b9293626768606196bf6188`
- Frontend TAR SHA-256:
  `91f56c73e775f1769686c714921d1a1afbd513ff19c563aac429137bf05e1e83`

Local inspection of that exact retained image confirmed:

- Architecture: `linux/amd64`
- Created: `2026-08-01T21:59:44.445671298Z`
- OCI revision: `3e83ff6904e7c3fb50164ec56d59b45e87e7e89c`
- `index.html` SHA-256:
  `bb4db4570cc6f2849a17d4aaa20ec81f4041da1f1858a1fbd1868ebc46ab9d53`
- Main CSS: `assets/index-pXH0ospM.css`
- Main CSS SHA-256:
  `afef75a0c510a3ab84adc1b2cf84e306806083b9cbb7ae7a62946ffaa0dec023`
- Form tokens in generated CSS: absent
- Service worker: `service-worker.js`
- Service-worker SHA-256:
  `2b8178e692513234705293992c94a451640bb9844e11eb03d0b6eed819861b57`

Local inspection of the refactored comparison image confirmed:

- Image ID:
  `sha256:8f0435a2a67317203cf4e412d248462323a715615b78fa4e4506fa5a13414886`
- Created: `2026-08-02T14:11:55.456982728Z`
- Architecture: `linux/amd64`
- Main CSS: `assets/index-DBjbp0or.css`
- Main CSS SHA-256:
  `a2c37e9ac18a7861a1b29c7a7fb9b5e95fa2b9ad6ba9207d5d06b7f3ee27f46d`
- Form tokens in generated CSS: present
- Service-worker SHA-256:
  `3c6844f5f644aab7c2ed7f6e2aa1231402fc671535eedafbecbe0204b8ae00a8`

The differing fingerprinted CSS names and service-worker hashes prove that the
two builds are distinct. A stale PWA cache can retain an old shell, but it
cannot explain why the authoritative image itself carries the old commit and
old CSS. The primary failure is image selection/build provenance.

## Deployed-host verification boundary

Direct verification of the currently running VPS container was attempted twice
using key-only, non-interactive SSH on `87.248.131.157:9011`; both attempts
timed out before a TCP connection was established. HTTP requests to port 80 for
`/`, `/service-worker.js`, and `/manifest.webmanifest` also timed out.

Consequently the running container ID, its live image ID, live asset hashes,
and live browser cache registrations could not be read during this audit. They
are **not** reported as passing. The exact retained deployment artifact is the
latest available authoritative evidence, but production must be rechecked when
the VPS is reachable.

## Real visual comparison

Real browser captures (not generated mockups) are stored under
`docs/screenshots/`:

- `form-ux-before-mobile.png`
- `form-ux-after-mobile.png`
- `form-refactor-verification/login-before-mobile.png`
- `form-refactor-verification/login-after-mobile.png`
- `form-refactor-verification/login-before-desktop.png`
- `form-refactor-verification/login-after-desktop.png`

The purchase comparison at 390px visibly shows the compacted controls, denser
item group, smaller dialog chrome, and preserved sticky footer. Login captures
were repeated after the initial splash state settled.

The requested authenticated customer, product, inventory, and long-dialog
matrix could not be completed reliably in this forensic run: the isolated
browser session's login remained on the login route despite the same local API
credentials succeeding via a direct API request. No screenshots were fabricated
or mislabeled. This is an explicit incomplete verification item, not a pass.

## Validation performed

- `npm test`: passed, 10/10
- `npm run type-check`: passed
- `npm run build`: passed, Vite 8.1.5, 1021 modules transformed
- Local old/refactored Docker image metadata and asset inspection: passed
- Production VPS SSH/HTTP inspection: blocked by network timeout
- Full authenticated screenshot matrix: incomplete as described above
- Docker rebuild for the audit: started, but exceeded the 180-second command
  window; the result is not counted as passed unless the tagged image is later
  confirmed

## Root cause and required release action

Root cause: the form refactor is in local `development`, but the recorded
production frontend image was built from an earlier commit. No application
source correction is justified by this finding, and no unrelated source was
changed during the audit.

Before claiming deployment success:

1. make `08c28d1` (and its contained `development` state) available to the
   controlled image build;
2. build the frontend with `VITE_BUILD_COMMIT` set to the final development SHA;
3. verify the OCI revision label and embedded frontend build metadata;
4. deploy that exact image ID;
5. confirm the live container image ID equals it;
6. confirm the live main CSS contains `--app-form-control-height`;
7. invalidate the old PWA shell by serving the new fingerprinted assets and
   verify `service-worker.js` with no-cache semantics;
8. repeat the authenticated 390px and desktop screenshot matrix against the
   reachable production host.

## Final repository state

The source refactor remains at `08c28d151870ab0f58e521ca617dbf54480ddef8`
on `development`. This report and its verification screenshots form the only
new repository changes from the forensic audit. The final development SHA is
recorded after the report commit.
