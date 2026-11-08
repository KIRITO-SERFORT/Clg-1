# Phase 1 changes (rebrand + preserve)

Package, namespace and applicationId are unchanged, so existing installs update in place. DataStore file name (`acgcet_widtz_prefs`),
WorkManager unique names and the notification channel id are also unchanged on purpose (renaming would orphan data/schedules).

## Rebrand to "Clg 1%"
`strings.xml` (app, widget picker, login), login screen, loading screen title, dashboard header/About card, widget labels, content descriptions,
notification text, bug-report subject/body, UPI note, test expectations, README, CI artifact names, Gradle root project name (`Clg1`).
Left unchanged on purpose (needs the owner's decision): the Ikegai Telos logo button, the developer credit lines, and the "ACGCET MECHANICAL-2026-2030 batch" box.

## Security / privacy
- `data/security/SecretCipher.kt`: Android Keystore AES-256-GCM.
- `UserPreferences`: password stored encrypted (`key_student_password_enc`). Old plaintext value is migrated once (encrypt, verify by decrypting, then delete plaintext).
  If the Keystore is unavailable the old value is kept so nobody is logged out unexpectedly; for NEW logins it fails closed (no plaintext storage).
  If decryption fails later (for example after a restore), the user is simply asked to log in again.
- Backup rules exclude `datastore/` from cloud backup and device transfer. Effect: after a reinstall/new phone the student logs in again.
  Rollback: restore the two XML files from git history.

## Reliability
- Worker cancels its schedule after a rejected password.
- Widget updates only trigger a sync if data is older than 15 minutes (`syncIfStale`).
- Stale marker after 12 h: compact widget header "Clg 1% · old data", Mini widget "Absent y% (old)" / "ATTENDANCE · OLD DATA". (Full widget already showed the sync time.)
- Splash can be skipped by tapping it.

## Integration boundary
`ErpConnector` interface + `ErpConnectors.create()`; `ErpClient` implements it unchanged. Repository, worker and timetable ViewModel use the interface.
Adding a college = new class implementing `ErpConnector` + a selector (not built yet).

## Configurable behaviour
- Shortage threshold: all text and the compact widget now follow the student's target slider (60-90 %).
- `DayOrderCalculator` accepts a set of teaching days (default Mon-Fri, so behaviour is unchanged). UI to choose teaching days is NOT built yet.

## Buy me a tea
After returning from a UPI app the app asks "Did your payment complete?" (Yes / No-not sure). Nothing is unlocked either way; the tea dialog says so.

## Tests
Run in this environment (real compiler, JVM): `DayOrderCalculator` (14 hand-calculated cases) and `WidgetFreshness` (4 cases): all passed.
Added to the project as JUnit: `DayOrderCalculatorTest`, `WidgetFreshnessTest`. Existing tests updated for the new names.
NOT run: the Gradle build, Robolectric tests, anything touching Android APIs (Keystore migration, widgets, notifications, UPI intent).

## Manual regression checklist (needs a phone)
1. Upgrade over an existing install: still logged in, attendance visible, then confirm the plaintext key is gone (second launch works).
2. Fresh install: login, background sync after 4 h, widgets render (3 types), timetable day order and "Fix day order".
3. Wrong password after a password change: sync stops, app asks to log in again.
4. Bug-report button opens an email draft and sends nothing. Tea button: no success message without tapping "Yes".
5. Airplane mode: app opens with last data; widgets show old-data marker after 12 h.

## Open items / owner decisions
Ikegai Telos button; Play-policy decision for the battery-exemption permission; remove unused dependencies; teaching-days setting UI; reduced-motion; 
Phase 0 build baseline in CI.
