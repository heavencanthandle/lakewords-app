# lake words

Read [README.md](README.md) for what the app is and how it is built.

## Do not

- Do not add anything that wipes e-ink ghosting (a black flash, a forced full refresh). The ghosting is wanted. Do not speed the intro up past a few frames a second (`Shore.FPS`), add grey, or animate the whole screen each frame. It is drawn for e-ink: black and white, small changes per frame, salt only adding black.
- Do not edit the book text by hand in `assets/lakewords.txt` without saying so. It is converted from the LakeWords website's read page; fix it there too, or note the difference.
- Do not load EB Garamond from Google Fonts at run time. The Kompakt has no Google services. The font files ship in `res/font/` with their license in `assets/`.
- Do not add permissions. The app has none, and no network.
- Phone updates replace the installed app: bump `versionCode`, then `adb install -r`, same certificate. The phone has the release build, signed by the key in `signing/` (gitignored; it signs debug builds too). Never replace or remove that key. Do not uninstall or clear data unless asked.
