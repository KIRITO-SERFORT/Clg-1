# Phase 0 audit (baseline: acgcet-widtz_v5.zip)

## Stack
- Kotlin 2.2.10, Jetpack Compose (Material 3, BOM 2024.09.00), AGP 9.1.1, Gradle 9.3.1, Java 11 target
- minSdk 24, targetSdk 36, compileSdk 36. applicationId `com.aistudio.acgcetwidtz.vkzpst` (kept), namespace/package `com.example`
- Glance 1.1.1 (widgets), WorkManager 2.9.1, DataStore Preferences 1.1.7, OkHttp 4.10.0, Jsoup 1.17.2, Coil 2.7.0 (optional loading GIF)
- Single module `:app`. Architecture: one ViewModel per area (`AttendanceViewModel`, `TimetableViewModel`) -> `AttendanceRepository` -> `ErpClient` + `UserPreferences`.
- Storage: DataStore Preferences only (attendance, subjects, timetable cached as JSON strings). **No Room database is used** even though Room is declared.
- Declared but unused in source: Room, Retrofit converter, Moshi, Firebase AI/App Check, logging-interceptor. No `Log.*`/`println` anywhere (good).

## Manifest inventory
- 1 activity (`MainActivity`), 3 widget receivers (full, compact pie, Royal Mini), 1 `BootReceiver`, 1 periodic worker, 1 notification channel. No services or providers.
- Permissions: INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. `<queries>` for `upi:` links.

## Features found (all present)
Loading animation (3.2 s minimum) with optional `res/raw/loading_logo.gif` override; login; 4 tabs (Overview, Subjects, Predictions/simulator, Settings);
one-day timetable with day-order anchor and "Fix day order"; 3 widgets; theme; target slider (60-90 %); shortage notifications; battery/autostart guidance;
contact + bug-report email draft; "Buy me a tea" UPI flow; Ikegai Telos link.

## ERP flow
Keycloak authorization-code login against `auth.tnedu.in` (realm `accet`), then `GET /student/attendance/{reg}`, `/student/attendance/subject/wise/record/{reg}`,
`/student/students/{reg}` (reads ONLY `nodeId`), `/student/time/table/{nodeId}/{reg}`. Fresh client and logout per call. Everything is hard-wired to ACGCET.

## Exact attendance formulas (preserved, unchanged)
- percentage = present * 100 / marked (0 when marked = 0)
- safeToMiss(t) = max(0, floor(present / t - marked + 1e-9)); hoursToRecover(t) = 0 if present/marked >= t, else ceil((t*marked - present)/(1 - t) - 1e-9)
- Subject versions use the same formulas with the subject's hours; `isBelowExamThreshold` = hours > 0 and percentage < t*100
- Day order: count Mon-Fri working days from a user-confirmed anchor date, modulo the timetable's `noOfDays`; weekends roll to Monday.

## Risks found
| # | Risk | Severity | Status |
|---|------|----------|--------|
| 1 | ERP password stored as **plaintext** in DataStore | High | Fixed in Phase 1 (Keystore AES-GCM + safe migration) |
| 2 | `allowBackup=true` with empty backup rules: roll number, name, attendance (and the old plaintext password) could go to cloud backup | High | Fixed: DataStore excluded from backup/transfer |
| 3 | Periodic worker kept retrying after "wrong password" (can lock the ERP account) | Medium | Fixed: schedule is cancelled |
| 4 | Every widget `onUpdate` triggered a full ERP login | Medium | Fixed: only if cached data is older than 15 min |
| 5 | Compact widget used a hard-coded 75 % instead of the student's target | Low | Fixed |
| 6 | Notification/Settings/Subjects text hard-coded "75 %" and "exam eligibility" | Low | Fixed: uses the student's target |
| 7 | UPI return showed "Thanks for the tea!" automatically (return is not proof of payment) | Medium | Fixed: neutral "Did your payment complete?" |
| 8 | 3.2 s forced splash on every start | Low | Mitigated: tap to skip. Reduced-motion not yet honoured |
| 9 | Mini and compact widgets had no stale-data indicator | Low | Fixed (text marker after 12 h) |
| 10 | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is restricted by Google Play policy | Medium (Play only) | Open: fine for sideloading; decide before a Play release |
| 11 | Unused heavy dependencies (Firebase AI/App Check, Room, Retrofit, Moshi) and `secrets`/`google-services` plugins | Low | Open: left alone because the AI Studio build environment may need them |
| 12 | `DayOrderCalculator` assumed Mon-Fri | Low | Engine now configurable; Settings UI not wired yet |
| 13 | Only ACGCET supported; error messages said "ACGCET portal" | Low | Connector interface added; messages made neutral |

## Build status of the baseline
NOT run in the audit environment (no Android SDK / Gradle and no access to Google/Maven repositories there). Run `gradle assembleDebug testDebugUnitTest` in CI
and record any pre-existing failures separately.
