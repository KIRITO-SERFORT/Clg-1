# Prompt to paste into Google AI Studio
(Attach: new_logo_for_clg_1_.jpeg and Clg1UiKit.kt)

```
I have an existing Android app: Kotlin, Jetpack Compose, Material 3, single MainActivity,
minSdk 24. I attached my logo (new_logo_for_clg_1_.jpeg) and a finished UI kit file (Clg1UiKit.kt).

GOAL: give the whole app a modern, smooth look by using the attached kit as the design system,
and add the logo loading animation. Do NOT redesign from scratch and do NOT invent a different style.

STEP 1 - Setup
- Copy the logo to app/src/main/res/drawable-nodpi/clg1_logo.jpeg (lowercase, do not convert/resize).
- Add Clg1UiKit.kt to my UI package (fix the package line and the `import com.example.R` if mine differs).
- No new libraries. Compose only.

STEP 2 - Apply
- Wrap the app root in ClgTheme { } and enableEdgeToEdge() in MainActivity.
- Show LoadingScreen(isReady = <my initial-state-loaded condition>, onFinished = { showLoading = false })
  on top of the content. Keep showLoading in rememberSaveable.
- Put every screen inside ClgBackground. Replace my bottom navigation with ClgBottomBar and my tab
  switching with ClgTabContent.
- Replace plain cards with GlassCard, primary buttons with GradientButton, circular/overall numbers with
  ProgressRing + AnimatedNumber, subject items with SubjectRow, small stats with StatCard.
- While data loads show SkeletonBox placeholders, never an empty screen or a bare spinner.
- Lists use Modifier.staggeredEnter(index, reduceMotion). Tappable things use bouncyClick (already
  inside GlassCard(onClick) and GradientButton).

STEP 3 - Rules (keep them)
- Keep all existing behaviour, ViewModels, navigation, data and business logic unchanged. Only the UI layer changes.
- Never show status with colour alone: always include a text label (Safe / Careful / Low ...).
- Respect reduced motion: pass rememberReduceMotion() to animations; when true skip decorative motion.
- Touch targets at least 48dp, contentDescription on icons/buttons, text readable at 200% font scale.
- Brand colours only from ClgColors (navy #010824, cyan #00F0FF). Dark is default; light must also look correct.
- Animations 200-500 ms, no jank on low-end phones, no allocations inside draw loops.

STEP 4 - Output
- Show every changed file as a diff, one screen at a time (start with MainActivity + Home screen), and wait for
  my OK before the next screen.
- List anything you could not verify (for example that it compiles). Do not invent APIs. If my project
  differs from these assumptions, ask before changing it.
```
