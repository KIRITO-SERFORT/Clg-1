# Clg 1%

Clg 1% is an Android app built with Kotlin and Jetpack Compose. The supplied Phase 1 project has been extended with a local academic command center while retaining the original ERP/attendance features.

## Main functionality

- ACGCET ERP login, attendance, timetable, widgets and background sync from Phase 1.
- Academic setup wizard with local draft/resume, multiple academic periods, assessment components, grading rules and sample calculation.
- Per-subject marks and assessment components, missing/absent/exempt status, weighted calculations, grade points/SGPA, target marks and what-if analysis.
- Exam calendar with date/time and opt-in reminders.
- Structure-only `.clg1template` import/export, QR generation and camera scanning.
- Historical term snapshots, term-trend chart, historical grade/category analytics, multi-page PDF reports and encrypted academic backup/restore.
- Room database schema v3 with non-destructive migrations and legacy preference recovery copy.
- Google Play subscription client and secure server-verification source.
- One-time DataVis CSV transfer backend source with manual CSV sharing fallback.

## Build a debug APK on GitHub

1. Push this project to a GitHub repository using the `main` or `master` branch.
2. Open **Actions → Build Clg 1% debug APK → Run workflow** (or push a commit).
3. Wait for `testDebugUnitTest`, `connectedDebugAndroidTest` (API 35 emulator UI/device suite) and `assembleDebug` to finish.
4. Download the `clg1-debug-apk` artifact, or download the APK from the generated GitHub Release.

The workflow uses JDK 21 and Gradle 9.3.1 and creates a temporary debug keystore. It does not create a Play Store release-signed APK. Do not use the generated debug APK for public production distribution.

## Optional production integrations

### Billing

See `docs/GOOGLE_PLAY_BILLING_SETUP.md` and `backend/google-play-verifier/`. Configure `CLG1_PLUS_PRODUCT_ID` as a GitHub repository variable and `CLG1_BILLING_VERIFY_URL` as a GitHub secret. Deploy the verifier with a service account JSON stored only as a hosting secret. Until these values are configured and the product is available in Play Console, the purchase button remains disabled and premium is not granted.

### DataVis

See `docs/DATAVIS_TRANSFER_SETUP.md` and `datavis-transfer/`. Deploy the Cloudflare Worker, then set `CLG1_DATAVIS_TRANSFER_API` as a GitHub repository variable. The DataVis app's source repository was not part of this ZIP, so merge `datavis-transfer/datavis-integration/streamlit_import.py` into the DataVis Streamlit app and configure `CLG1_TRANSFER_API_BASE` there. Manual CSV sharing remains available without this backend.

## Local build

Requirements: Android Studio with Android SDK 36 extension 1, JDK 21 and Gradle 9.3.1. From the project root run `gradle testDebugUnitTest` and `gradle assembleDebug`. On Windows use `scripts/build-debug.ps1`; on macOS/Linux use `scripts/build-debug.sh`. The app module output is `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy and data

Academic workspace data is local-only and mirrored into Room. Templates contain academic structure only. CSV sharing and DataVis transfer require explicit user action. Encrypted backup is passphrase protected and includes local academic history. ERP credentials remain in the existing Android Keystore-backed storage and are not included in academic templates, CSV exports or academic backups.

## Current verification limits

The source ZIP has been structurally checked and pure Kotlin academic logic has been compiled/tested. The current environment lacked Android SDK/Gradle and network access, so a complete Android Gradle build, device run, camera test, Play Billing test and deployed DataVis end-to-end test could not be performed. The Android UI/device regression phase is intentionally not marked complete. Run the GitHub Actions workflow and resolve all reported errors before release.

See `docs/IMPLEMENTATION_STATUS.md` for the phase-by-phase status, `docs/ANDROID_REGRESSION_CHECKLIST.md` for automated and manual device checks, and `docs/LOCAL_VERIFICATION_LOG.md` for checks actually run.
