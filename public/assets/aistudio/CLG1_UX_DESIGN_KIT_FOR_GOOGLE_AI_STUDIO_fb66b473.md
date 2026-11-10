# Clg 1% — UX Design Kit for Google AI Studio

Paste this entire document into Google AI Studio (or attach it) together with the Kotlin file `UxProfile.kt` in this same folder. App: **Clg 1%**, repo `KIRITO-SERFORT/Clg-1`, package `com.example`, Jetpack Compose + Material 3.

---

## Prompt you must obey (read first)

You are editing the Android app **Clg 1%**. Do the following and nothing else:

1. **KEEP the existing UX.** Do not restyle, rename, reorder, or delete current screens, navigation, copy, colours, widgets, ERP flows, academic wizard, billing, or Room data. Existing behaviour is the baseline called **UX-1 Ledger** and it is the **default**.
2. **Do not replace** `ClgUxKit.kt`, `ClgThemes.kt`, `Clg1UiKit.kt`, `Theme.kt`, or any screen. Those files stay. You may *call* their composables.
3. **Add a Settings section** titled **Experience** with three radio options. Users can switch at any time. The choice is saved on-device and applied immediately. Switching UX must never delete marks, attendance, timetable, credentials, or billing state.
4. **UX profiles only change presentation** (spacing density, motion, emphasis of numbers, empty/loading treatment). They must not change calculations, ERP parsing, pass rules, or what data is stored.
5. Colours still come from the existing palette picker (`ClgPalette` + `ThemeMode`). A UX profile is **not** a colour theme. Do not add a second colour system.
6. Respect reduced motion: if `rememberUxReduceMotion()` is true, skip non-essential animation in every profile.
7. If a screen is not listed below, leave it visually identical under all three profiles.
8. Do not add new libraries. Compose + Material 3 only.
9. Do not unlock premium features. Do not touch Keystore, ERP password, or billing verification.

If a change would alter Ledger’s pixels when `UxProfile.LEDGER` is selected, do not make that change.

---

## What the app already is

College companion. Two worlds:

- **ERP attendance (ACGCET only):** login, overall + subject attendance, safe-to-miss / recover hours, timetable with day-order, widgets, shortage notifications.
- **Academic workspace (any college, offline):** setup wizard, marks, weighted rules, SGPA/CGPA, exam command center, templates (`.clg1template`), encrypted backup, DataVis/CSV export.

Stack: Kotlin, Jetpack Compose, Material 3, Room, DataStore/SharedPreferences, WorkManager, Glance widgets.

### Existing UX pieces you must reuse

| File | Role |
|---|---|
| `app/src/main/java/com/example/ui/ux/ClgUxKit.kt` | Feel layer: skeletons, empty, error+retry, count-up, undo snackbar, swipe-delete, bottom sheet, large scaffold, field, first-run tip, haptics, reduced motion |
| `app/src/main/java/com/example/ui/theme/ClgThemes.kt` | Palettes: Midnight Cyan, Aurora, Forest, Sunset, Ocean, Sakura, Graphite, AMOLED Black, Purple, Black & Red, Paper, Material You. Prefs file `clg1_theme`, key `palette`. Picker: `ClgThemePicker` |
| `app/src/main/java/com/example/ui/theme/Theme.kt` + `Color.kt` + `Type.kt` | MaterialTheme wrapper, navy defaults, typography |
| `app/src/main/java/com/example/ui/kit/Clg1UiKit.kt` | Older UI kit. Do not fork it into a third theme |
| `UserPreferences.KEY_THEME_MODE` | `SYSTEM` / `LIGHT` / `DARK`, default dark |

Public helpers already in `ClgUxKit.kt` (call these, do not reimplement):

`rememberUxReduceMotion`, `rememberUxHaptics`, `uxShimmer`, `UxSkeletonRow`, `UxSkeletonList`, `UxState`, `UxStateHost`, `UxEmptyState`, `UxErrorState`, `UxSuccessCheck`, `UxCountUp`, `SnackbarHostState.showUndo`, `UxScreenSwitcher`, `UxSwipeToDelete`, `UxPullToRefresh`, `UxBottomSheet`, `UxLargeScaffold`, `UxField`, `UxFirstRunTip`, `resetUxTips`.

### Screens that exist (do not rename routes)

`LoadingScreen`, `OnboardingScreen`, `LoginScreen`, `DashboardScreen`, `TimetableCard`, `AcademicSetupWizard`, `CgpaCalculatorScreen`, `ExamCommandCenterScreen`, `AcademicTrendChart`, `ChatsScreen`, `PremiumUpgradeScreen`, `AppUpdateDialog`.

---

## UX profiles (Settings labels)

Store with the code in `UxProfile.kt`. Preference file `clg1_ux`, key `ux_profile`, default `ledger`.

| Settings label | Enum | id | What the user feels | What you may change |
|---|---|---|---|---|
| **UX-1 Ledger** | `UxProfile.LEDGER` | `ledger` | Today’s app. Dense enough for marks, familiar cards. | **Nothing.** This is a no-op branch. |
| **UX-2 Focus Desk** | `UxProfile.FOCUS_DESK` | `focus_desk` | One job at a time. Bigger type on the number that matters, extra spacing, fewer simultaneous cards, tips hidden after first dismiss. | Padding scale 1.15, headline emphasis via `UxCountUp` on attendance % and SGPA only, collapse secondary cards behind “More” using existing `UxBottomSheet`. Same colours, same order of primary actions. |
| **UX-3 Glance Board** | `UxProfile.GLANCE_BOARD` | `glance_board` | Home looks like the widget: huge attendance and next class, shorter labels, skeletons via `UxSkeletonList` instead of spinners. | Dashboard hero only. Other screens stay Ledger. Use `UxLargeScaffold` title “Today”. |

Names to show in Settings, exactly:

- `UX-1 Ledger` — subtitle: `Current layout. Nothing moves.`
- `UX-2 Focus Desk` — subtitle: `Calmer spacing. One number at a time.`
- `UX-3 Glance Board` — subtitle: `Home looks like the widget.`

Put this block **under the existing theme picker**, not instead of it. Title: **Experience**. Helper text: `Colour themes stay as they are. This only changes layout feel. Ledger is the original.`

---

## Screen rules per profile

### Global

- Ledger: render the current composable unchanged. Wrap with `when (profile)` and put today’s code in the `LEDGER` (and `else`) branch.
- All profiles: `UxStateHost` for load/error if the screen already has a loading flag. Do not invent new error copy; keep current strings.
- Haptics: `rememberUxHaptics().tick()` on profile change only.
- Accessibility: 48dp minimum touch targets. Do not rely on colour alone for shortage vs safe attendance (keep the existing words “short” / “safe”).

### Dashboard

- Ledger: current `DashboardScreen`.
- Focus Desk: same data, vertical rhythm +4.dp over current, attendance percentage uses `UxCountUp`, subject list shows top 3 then “All subjects” opens `UxBottomSheet`.
- Glance Board: first viewport is only (1) overall attendance, (2) next class from timetable, (3) one line if any subject is under the user’s target threshold. Everything else below the fold, unchanged components.

### Login, onboarding, wizard, CGPA, exams, chats, premium

- Ledger look on all three profiles.
- Allowed additive UX only, same in every profile, and only if the screen currently has a blank spinner or a dead-end error:
  - loading → `UxSkeletonList`
  - empty → `UxEmptyState` with the screen’s existing message
  - failure → `UxErrorState` with the existing retry lambda
- Do not add tips that block the ERP login or the setup wizard.

### Widgets

- Do not change Glance widget layouts in this task.

---

## Integration steps (do these in order)

1. Add `app/src/main/java/com/example/ui/ux/UxProfile.kt` exactly as supplied. Do not rename ids.
2. In the screen that already hosts `ClgThemePicker` (Settings / appearance), add `UxProfilePicker` directly **below** it.
3. Hoist `UxProfile` from `UxProfileStore` (same way `ClgPalette` is hoisted from `ClgThemeStore`) into `MainActivity` and pass it only to `DashboardScreen` plus the Settings picker. Other screens ignore it.
4. In `DashboardScreen`, branch on profile as specified. Default and unknown values render the existing UI.
5. Add one unit test: `UxProfile.fromId("nope") == LEDGER`, `fromId("focus_desk") == FOCUS_DESK`, `fromId(null) == LEDGER`.
6. Do not bump schema, do not migrate Room, do not change `AndroidManifest`.

---

## Acceptance

- Fresh install opens **UX-1 Ledger** and looks identical to the build before this change.
- Settings shows three options. Switching to Focus Desk or Glance Board updates Home immediately and survives process death.
- Switching back to Ledger restores the old Home pixel-for-pixel (same composables).
- Theme picker still changes colours independently of UX profile.
- Airplane mode, no ERP login, and empty academic store still show the existing empty copy, not a new illustration pack.
- No new Gradle dependencies.

---

## Kotlin to add

The file content is in `UxProfile.kt` next to this document. Place it at:

`app/src/main/java/com/example/ui/ux/UxProfile.kt`
