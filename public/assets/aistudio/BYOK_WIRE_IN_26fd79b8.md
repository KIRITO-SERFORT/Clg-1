# Wire BYOK into AI analytics

Add [ByokAi.kt](ByokAi.kt) at:

`app/src/main/java/com/example/data/analytics/ByokAi.kt`

OkHttp and `org.json` are already on the app. No new Gradle dependency. The key is encrypted with the existing `SecretCipher` (Android Keystore). It is not logged and not included in academic backups.

## 1. Title row (settings icon)

Next to the existing "AI analytics" label, replace the plain title with:

```kotlin
AiAnalyticsTitleRow(context = context)
```

That draws the label and a gear. The gear opens Gemini / ChatGPT / DeepSeek / Claude and a masked key field.

## 2. Use the key inside `generateAcademicStrategy`

At the top of `AiAcademicAdvisorService.generateAcademicStrategy`, pass a `Context` (or read the key in the ViewModel and pass provider + key in). Preferred call site is the ViewModel, so the service stays free of Context:

```kotlin
val byokKey = ByokStore.loadKey(context)
val byokProvider = ByokStore.loadProvider(context)
val prompt = /* same text buildAdvisorPrompt already builds */
val text = if (byokKey != null) {
    ByokAdvisorClient().complete(byokProvider, byokKey, prompt)
} else {
    // existing Firebase Gemini path — do not remove it
    existingFirebaseGenerate(prompt)
}
```

Make `buildAdvisorPrompt` internal (or duplicate the call) so both paths share one prompt.

## 3. Rules

- No saved key: behaviour is unchanged (Firebase Gemini).
- Bad key or HTTP error: show the existing AI analytics error state. Do not fall through silently in a way that spends the built-in model after the user chose BYOK.
- Clear key: Remove in the dialog deletes `clg1_byok` and the next run uses built-in Gemini again.
- Do not put the key in Room, CSV, templates, logs, or crash reports.
