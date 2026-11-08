# Clg 1%

![Build Clg 1% debug APK](https://github.com/KIRITO-SERFORT/Clg-1/actions/workflows/build-apk.yml/badge.svg)

Android academic companion for college students — ERP attendance, timetable and widgets plus a local-first academic workspace (marks, SGPA/CGPA, exams, templates, backup, DataVis export). Formerly "ACGCET WIDTZ"; an independent student project, not IKEGAI TELOS.

> Status: source complete, CI build in progress. No code changes in this pass — this update refreshes the repo presentation (README/About) only. A debug APK will be published here only after the GitHub Actions build is green.

## What the app does

- **ERP attendance (ACGCET only):** login, overall + subject-wise attendance, safe-to-miss / recover-hours maths, timetable with day-order + fix-day-order, background sync, shortage notifications, full/compact/Royal Mini widgets.
- **Academic workspace (all colleges, offline):** setup wizard with draft/resume, multiple periods, assessment components (tests, assignments, quizzes, labs, practicals, viva, projects, end-sem), weighted/normalized combination rules, attendance-slab marks or eligibility-only, grade bands + grade points, SGPA/CGPA, provisional vs final results with plain-language calculation paths.
- **Exam Command Center:** exam calendar + countdown, opt-in WorkManager reminders (rescheduled on reboot), what-if / "what do I need?" target solver that never overwrites real marks, CSV import preview, shareable report, delete-my-data.
- **Templates:** structure-only `.clg1template` (JSON) export/import via system share sheet or QR, strict schema + checksum validation, "community — not institutionally verified" preview, merge adds subjects without touching marks or ERP secrets.
- **Data & privacy:** academic records stay on-device (Room + legacy SharedPreferences mirror); passphrase-encrypted backup/restore; CSV/DataVis export only after explicit user review; no cloud sync; ERP password stays in Android Keystore.
- **Freemium:** basic marks, calculations and records are free. Advanced analytics/PDF/history are gated by verified Play Billing (server-verified); a local toggle never unlocks them. "Buy me a tea" (UPI) is a donation, never proof of purchase.

## College support

- Attendance sync works with the **ACGCET ERP only**. Other colleges can be added later through `ErpConnector`; the academic workspace (setup, marks, SGPA/CGPA, exams, templates) already works for any college without ERP login.

## Build a debug APK (GitHub Actions is the supported path)

1. Open **Actions → Build Clg 1% debug APK → Run workflow** (or push to `main`).
2. Wait for `testDebugUnitTest`, `connectedDebugAndroidTest` (API 35 emulator UI/device suite) and `assembleDebug`.
3. Download the `clg1-debug-apk` artifact, or the APK attached to the auto-generated GitHub Release. Do not distribute a debug APK as production.

Local build needs Android Studio + SDK 36 ext 1, JDK 21, Gradle 9.3.1: run `gradle testDebugUnitTest`, then `gradle assembleDebug` (Windows: `scripts/build-debug.ps1`, macOS/Linux: `scripts/build-debug.sh`). Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy & safety

- ERP password: Android Keystore-encrypted, on-device, excluded from backups. Academic records, CSVs and templates never include name, roll number, ERP credentials, tokens or device IDs unless the student types them into free-text fields.
- Do not hard-code your college's official rules from the in-app examples: verify grading, pass and credit rules with your institution before relying on any GPA/result.

## Docs (start here)

- `docs/PHASE0_AUDIT.md` — repo audit, stack, risks.
- `docs/PHASE1_CHANGES.md` — preserve + rebrand notes, regression checklist.
- `docs/IMPLEMENTATION_STATUS.md` — phase-by-phase status with evidence (read this before claiming "done").
- `docs/ANDROID_REGRESSION_CHECKLIST.md` — automated + manual device checks.
- `docs/ACADEMIC_DATA_SCHEMA.md` — local models + template/CSV schemas.
- `docs/DATAVIS_CSV_SCHEMA.md` + `docs/DATAVIS_TRANSFER_SETUP.md` — DataVis manual + one-time transfer setup.
- `docs/GOOGLE_PLAY_BILLING_SETUP.md` + `backend/google-play-verifier/` — billing setup.
- `docs/LOCAL_VERIFICATION_LOG.md` — checks actually run vs not run.
- `docs/ACADEMIC_SETUP_QR_HISTORY_ROOM.md` — QR/template/history/Room notes.

## Production integrations (optional, owner-configured)

- **Billing:** set `CLG1_PLUS_PRODUCT_ID` (repo variable) + `CLG1_BILLING_VERIFY_URL` (secret), deploy the verifier; purchase stays disabled until configured. No client-side boolean unlocks premium.
- **DataVis:** deploy the Cloudflare Worker, set `CLG1_DATAVIS_TRANSFER_API`, merge `datavis-transfer/datavis-integration/streamlit_import.py` into the DataVis app; manual CSV sharing works without it.

## Release honesty

- A release is "ready" only when Actions is green (unit + device tests + `assembleDebug`), an upgrade-over-install regression passes, and the APK installs and opens offline. Until then this repo is source-complete, not release-certified.
