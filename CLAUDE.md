# lake words

@AGENTS.md

The parent folder's `../CLAUDE.md` has the cross-app rules. This app talks to no other app.

## Build, test, phone

- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Tests: `ShoreTest` (the intro's
  rules: water recedes, salt only on dry ground and off the words, words darken steadily,
  finishes on time; bones only on dry ground; the tagline page only adds ink), `BookTest`.
- Phone: the debug certificate. Raise `versionCode`, then
  `adb -d install -r app\build\outputs\apk\debug\app-debug.apk`.
- To see the intro, `adb exec-out screencap -p` at a few seconds apart works well: it runs
  about 36 s, then the cleared screen and the tagline scene, about 17 s more. Another session
  using the phone at the same time can bring its own app to the front; check what is on top.

## Conventions

- Sources in `app/src/main/kotlin`: `intro/Shore.kt` (the shore, pure Kotlin, no Android),
  `intro/Salt.kt` (dendrites and hoppers), `intro/Bones.kt` (bird skull and skeleton line work),
  `intro/TaglinePage.kt` (the tagline and its pieces, drawn at full resolution),
  `intro/IntroScreen.kt` (draws it, makes the title mask from the font), `book/` (text and
  reader), `ui/Type.kt` (EB Garamond, black and white).
- Tuning lives in `Shore`'s companion: timings in seconds, crystal counts and sizes, wave
  spacing. The book text comes from the website: re-run the conversion if the site changes.
- Copy is lowercase, as on the website.
