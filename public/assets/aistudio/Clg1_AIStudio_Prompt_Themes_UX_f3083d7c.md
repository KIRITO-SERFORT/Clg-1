# Prompt for Google AI Studio: themes + UX kit
Attach: ClgThemes.kt, ClgUxKit.kt, ClgThemesTest.kt (and your project files if AI Studio asks).

```
I have an existing Android app (Kotlin, Jetpack Compose, Material 3, single MainActivity, minSdk 24).
I attached three finished files. USE THEM EXACTLY AS WRITTEN. Do not rewrite, "improve" or re-style them.

FILES
- ClgThemes.kt      -> app/src/main/java/com/example/ui/theme/ClgThemes.kt        (package com.example.ui.theme)
- ClgUxKit.kt       -> app/src/main/java/com/example/ui/ux/ClgUxKit.kt            (package com.example.ui.ux)
- ClgThemesTest.kt  -> app/src/test/java/com/example/ui/theme/ClgThemesTest.kt    (JUnit 4; it checks that every theme has readable contrast)
No new libraries. If a Material 3 API in the files does not exist in my Compose BOM version, tell me which one
and propose the smallest fix; do not silently replace the whole component.

TASK 1: extra colour themes
My project already has `MyApplicationTheme(themeMode: ThemeMode, dynamicColor: Boolean, content)` in ui/theme/Theme.kt and
ThemeMode (SYSTEM/LIGHT/DARK) saved in UserPreferences. Keep ThemeMode working exactly as it does now.
1. Change MyApplicationTheme to take `palette: ClgPalette = ClgPalette.BRAND` and delegate:
     ClgPaletteTheme(themeMode = themeMode, palette = palette, typography = Typography, content = content)
   Remove the old colour-scheme code that becomes unused. Keep the old `dynamicColor` parameter only if other code calls it.
2. In MainActivity: `val (palette, setPalette) = rememberClgPalette()` and pass `palette` into MyApplicationTheme.
3. In Settings, replace the existing theme-mode chips with:
     ClgThemePicker(selected = palette, onSelect = setPalette, themeMode = themeMode, onModeChange = <my existing setter>)
4. Every screen must use MaterialTheme.colorScheme roles (primary, surface, surfaceContainer, onSurface, ...). Find any hard-coded
   Color(0x...) in my UI files and replace it with the right colour role so ALL themes look right. List each replacement.
5. Verify visually that text stays readable in every theme in light and dark (the tests cover the colour maths, not my screens).

TASK 2: UX kit
Use ClgUxKit.kt to improve feel without changing behaviour:
- Wrap each data screen's content in UxStateHost(state, onRetry) with a UiState from the ViewModel (UxState.Loading / Error / Content),
  so users see a shimmer skeleton, then content, never a blank screen or a bare spinner.
- Lists that can be empty (exams, subjects, notes) use UxEmptyState with a helpful message and one action button.
- Swipe-to-delete rows use UxSwipeToDelete + snackbarHostState.showUndo(...): delete in the UI immediately, commit after the snackbar times out
  or restore on Undo. Every swipe-to-delete row MUST also have a visible delete button or menu item (screen-reader users cannot swipe).
- Tab changes use UxScreenSwitcher(index). Pull-to-refresh on the attendance screen uses UxPullToRefresh.
- Forms (add exam, add subject, calculator inputs) use UxField for inline error text, counters and keyboard Next/Done.
- Show UxFirstRunTip once on the Overview, Exams and Settings screens (short, one sentence each) and add a "Show tips again" button
  in Settings that calls resetUxTips(context).
- After saving something important (exam saved, template imported) show UxSuccessCheck briefly and call rememberUxHaptics().confirm().
- Respect reduced motion: the kit already checks rememberUxReduceMotion(); do not add extra animations that ignore it.

RULES
- Do not change ViewModels, data, navigation, business logic or ERP/Firebase code. UI layer only.
- Status is always shown as text as well as colour. Touch targets >= 48dp, contentDescription on icon-only buttons, readable at 200% font size.
- Work one screen at a time. After each, show the diff and wait for my OK.
- At the end list anything you could NOT verify (for example that it compiles or how it looks on a device). Do not invent APIs.
```
