# UI, Exam section and UPI changes

## What changed
**Exam section** (`ui/screens/ExamHub.kt`, edits in `ExamCommandCenterScreen.kt`)
- Hero "Next exam" card with a live countdown (refreshes every 30 s).
- Timeline grouped into Today / This week / Later / Past (Past is collapsed).
- Real date and time pickers (no more typing dd/MM/yyyy). Edit an exam (before, you could only delete). Confirm before removing.
- Reminders fixed: before, an exam less than a day away fired an instant notification. Now two reminders (1 day and 2 hours before) are scheduled and any whose time has already passed is skipped. Clearing data / restoring / applying a template now cancels the old reminders.
- Pure logic in `domain/ExamTimelineLogic.kt` (unit tested, incl. DST and the date-picker UTC trap).

**UI**
- Floating animated bottom bar (`ui/kit/ClgBottomBar.kt`) and animated tab transitions in `DashboardScreen`.
- Uses the app's own Material colours, text labels and content descriptions (not colour alone).

**UPI "Buy me a tea"** (`ui/screens/SupportDialog.kt`, `util/UpiLink.kt`)
- Amount chips (10/20/50/100/any) sent with the link; installed UPI apps are listed and opened directly; the app's result is read and shown honestly; Copy UPI ID and a QR code are always available as fallbacks.
- Link is a plain person-to-person link (no merchant code), validated and percent-encoded; note text is cleaned (the old note contained "%").
- Recipient is set in one place: `SupportConfig` in `SupportDialog.kt`.

## Tests run
`UpiLinkTest` (8) and `ExamTimelineLogicTest` (8): 16 passed on a JVM with the Kotlin 2.2.10 compiler.

## NOT verified
No Android SDK/Gradle here: the Compose code, manifest interaction and WorkManager scheduling were never compiled or run. No UPI payment was made, so it is unknown whether `arunsanna071@okaxis` receives money or whether a given UPI app accepts the link. Build with `gradle testDebugUnitTest assembleDebug`, then follow the manual checklist.

## Manual checklist
1. Settings > Buy me a tea: apps listed with icons; each opens its pay screen with payee "Arun Sanna" and the chosen amount; cancel and success both show a message; Copy ID and Show QR work; with all UPI apps disabled the dialog says none found.
2. Pay Rs 10 with a real UPI app and confirm it arrives. If an app refuses ("cannot pay this merchant/link"), that is the app's risk policy: use QR/Copy ID, or move to a payment gateway (below).
3. Exams: add (pickers), edit, remove; an exam in 5 hours schedules only the 2-hour reminder; reboot keeps reminders.
4. Bottom bar: every tab opens, TalkBack reads "selected", 200% font does not clip labels.
