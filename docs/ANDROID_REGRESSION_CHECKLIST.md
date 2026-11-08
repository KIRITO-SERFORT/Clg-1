# Android UI and device regression plan

## Automated tests included in this source

The GitHub Actions workflow runs both `testDebugUnitTest` and `connectedDebugAndroidTest` on an API 35 Google APIs emulator before publishing the debug APK.

- Academic setup wizard: all four steps, final profile validation/save, and saving an incomplete draft without NaN/Infinity values.
- Main activity smoke test: real launcher Activity renders Clg 1% branding on an emulator.
- QR runtime test: Android graphics runtime creates the QR bitmap and the compressed payload round-trips.
- Academic QR unit tests: payload round-trip, unsupported prefix rejection, bitmap dimensions and decompression-size protection.
- Historical analytics: chronological term series, trend deltas, historical grade distribution, category averages, CGPA and risk flags.
- PDF report: PDF signature/file generation and empty-history rejection.
- Room migration: a version-1 database is upgraded through migrations 1→2→3 while preserving its existing profile row.
- Legacy preferences migration: old academic workspace rows are imported into Room while the SharedPreferences recovery copy is retained.

## Manual checks still required on physical devices

These are not claimed as completed by a source-only build:

1. Install the GitHub Actions debug APK on API 24, API 29, API 33, API 35 and API 36 devices/emulators where available.
2. Launch, background, resume, rotate and force-stop/relaunch the app; confirm no crash or lost academic workspace.
3. Complete and resume the setup wizard; verify period dates, component weights, pass thresholds and grade boundaries against the college's official regulations.
4. Generate a QR template, scan it using the physical camera, cancel/retry the scanner, deny camera permission, then grant it and retry.
5. Import a QR and `.clg1template` file; verify the preview, duplicate handling and preservation of existing subject marks.
6. Archive at least three terms; inspect term trend chart, CGPA, grade distribution, category averages, subject deltas and risk flags; generate and open a multi-page PDF.
7. Upgrade a real previous-install database and confirm old academic data remains intact. Verify backup/restore and recovery-copy behavior.
8. Test large datasets, long subject/component names, empty/malformed template files, corrupted QR data, low storage and process death during file export.
9. Verify notification permission flows, scheduled reminders and boot rescheduling on supported Android versions.
10. Review accessibility font scaling, TalkBack labels, dark/light themes and narrow-screen layouts.

The GitHub emulator tests are automated coverage, not a substitute for the physical-device checklist above. Do not publish until the CI workflow passes and the manual checks relevant to the release device range are completed.
