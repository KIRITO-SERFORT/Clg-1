# Clg 1% implementation status

Updated: 2026-11-08. This document distinguishes source implementation from integrations that still require the user's Play Console, hosting credentials, external DataVis repository or device testing.

| Area / phase | Source status | Notes / external dependency |
|---|---|---|
| Phase 1 – ERP and core app | Preserved from supplied Phase 1 | Existing ACGCET login/attendance/timetable, widgets, background sync, settings and encrypted ERP credential storage retained. Live ERP credentials/network behavior were not testable here. |
| Academic rules and marks | Implemented | Weighted/average/best-N/highest/sum calculations, per-subject assessment components, missing/absent/exempt handling, component pass thresholds, grades, credits/SGPA, target calculator and non-mutating what-if calculations. Verify all rule values against the official college handbook. |
| Multi-step setup wizard | Implemented in source | Four steps, multi-period setup, component weights/maxima/pass thresholds, grading bands, validation, local draft/resume and fictional sample calculation. Current screen activates the first configured period. Internal/practical/external subtotals are shown; institution-specific separate theory/practical pass-fail gates are not yet implemented. |
| QR template generation/scanning | Implemented in source | Compressed QR generation, camera scanning, checksum/schema parse, preview before merge, structure-only export. `.clg1template` file sharing remains the fallback for dense QR payloads. Camera scanning requires device permission. |
| Historical analytics/PDF | Implemented in source | Term-by-term trend series and chart, average/SGPA deltas, credit-weighted CGPA, latest and all-term grade distributions, latest and historical assessment-category averages, subject deltas, risk flags and multi-page PDF reports. Historical data appears after a term is archived. Ten PDF generations are free; additional generation requires online Play verification. |
| Room database migration | Implemented in source | Room schema v3, non-destructive 1→2 and 2→3 migrations, SharedPreferences-to-Room migration, retained recovery copy, debounced dual-write mirror, history backup/restore and explicit academic-data deletion. A legacy-preferences-to-Room test is authored but has not been run; the Room schema upgrade migrations have not been executed here. |
| QR and file template interchange | Implemented in source | Checksummed schema validation, preview and merge that preserves existing subject marks. Community templates are clearly marked unverified. |
| Encrypted academic backup | Implemented in source | AES-GCM + PBKDF2-HMAC-SHA256; includes current workspace/profile/history. Restore replaces academic workspace only, not ERP credentials or attendance. |
| Google Play Billing | Client + verifier source implemented; production setup pending | Billing Library 9.1.0, product query, purchase/restore/pending flow, Keystore-protected token and server verification against Google Play Developer API. Requires a real Play Console subscription/product ID, deployed HTTPS verifier, service-account secret, internal testing and operational hardening before public production use. |
| DataVis transfer backend | Worker and Android client source implemented; external app merge/deployment pending | Cloudflare Worker + R2/D1/KV one-time transfer backend, 10-minute expiry, single-use token and cleanup. The DataVis repository was not supplied; a Streamlit integration helper is included but must be merged into DataVis and deployed. Manual CSV share remains available. |
| Build automation | Implemented | GitHub Actions builds a debug APK and uploads it as an artifact/release. Optional Billing and DataVis settings are read from GitHub variables/secrets. |
| Android UI/device regression suite | Implemented in source; execution pending CI/device access | Added Compose wizard UI tests, real Activity launch smoke test, Android QR graphics test, PDF generation test, Room 1→3 migration test, legacy preference migration test and expanded analytics/QR unit tests. GitHub Actions now runs unit tests plus connected emulator tests before APK publication. Physical-camera QR scanning, multi-API device coverage and other manual checks remain in `docs/ANDROID_REGRESSION_CHECKLIST.md`. |

## Verification actually performed in this environment

- Pure Kotlin academic domain sources compiled with the locally available `kotlinc` and a standalone harness passed 12 checks (weighted normalization, grade boundary, missing marks, target solver, impossible target, what-if result/non-mutation, setup validation and SGPA).
- Encrypted backup utility previously passed an AES-GCM round-trip and wrong-passphrase check with an Android Base64 compatibility stub.
- All Android XML resource files parsed successfully.
- Kotlin compiler parser pass reported no Kotlin syntax/parser errors; Android/Compose/Room dependency type-checking was not possible without Gradle/SDK dependencies.
- Added automated UI, emulator and unit-test sources, but they have not been executed in this environment.
- Kotlin source parsing produced no Kotlin syntax/parser errors in the available compiler pass. That pass did not have Android/Compose/Room/Play Billing dependencies, so it is not a type-check or Android build.
- Node syntax check is run for the billing verifier; the Cloudflare Worker still needs Wrangler typecheck/deploy in its target environment.

## Important build limitation

This workspace did not have the Android SDK, Gradle executable/wrapper JAR, or network access to download them. Therefore no APK was built here and the Android project has not been certified as compile-clean or crash-free. Use the included GitHub Actions workflow to run the real Android build and unit tests; fix any CI-reported errors before distributing the APK. Do not treat a successful ZIP check or pure Kotlin test as proof of a full Android build.

## Remaining non-testing work / external dependencies

- The supplied ERP integration remains the ACGCET connector. A generic connector interface exists, but no second college ERP adapter or multi-profile college switcher has been delivered.
- Attendance remains in the existing ERP attendance area. There is no attendance-to-marks formula or eligibility-only calculation wired into the academic rules; no attendance marks are fabricated or exported (the DataVis attendance columns are blank).
- Internal, practical/lab and external subtotals are calculated and displayed by component category. Institution-specific separate theory/practical pass gates and a configurable composite theory/practical formula still need the official rule definitions.
- DataVis's actual source repository was not included, so only the secure transfer Worker, Android client and a Streamlit integration helper could be added. The helper still needs merging into the deployed DataVis app's existing validation pipeline.
- Google Play Billing source is present, but a real subscription product, service-account secret, HTTPS verifier deployment, Play license testing and public release configuration must be supplied by the project owner.
- A production release-signed APK requires a private upload keystore and secrets; the included workflow intentionally creates a debug APK only.
