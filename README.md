# lake words

*lake words* for the Mudita Kompakt: a queer, abolitionist love letter for great salt lake, a
love so robust it refuses to look away.

On each start it opens on the shore, seen from above. The water laps, then draws back down the
screen. Once it passes the middle, the words *lake words* come up faintly. Salt crystals grow
out of them, and over the bed the water has left, where a bird's skeleton lies low on the shore: come a little apart, half under sand and faint,
ragged feathers still hanging off the wing.
The words darken as the salt grows. The screen clears to white, the tagline comes up large in
italic, then under it, one at a time, the knot band and the vigil keepers from the website's
zine-launch handbill, and a little bird skull. Then the book. Five quick presses skip to the book. In the book, **replay** (small, top
right beside the title) plays it again.

The book turns by pages: press the lower part of the screen to go on, the upper part to go
back. A drag still scrolls.

## Drawn for e-ink

The intro moves four times a second, in black and white only, on cells of two pixels. The
stipple in the water is fixed to the ground, so only the wave lines, the water's edge, and new
salt change between frames. Salt only ever adds black. Shades (the faint words) are ordered
dither, not grey. Nothing wipes the panel: the Kompakt's ghosting of what came before is part
of the picture, so the clear before the tagline is to white only. The salt is two kinds of crystal (`intro/Salt.kt`): branching dendrites, by
diffusion-limited aggregation on a square lattice with noise reduction, and hoppers, the hollow
stepped squares halite makes, growing a frame at a time. The screen stays on during the intro only.
Each run is new.

## Text and type

- The text is the website's read page (`LakeWords.github.io/docs/read-lake-words`), turned into
  `app/src/main/assets/lakewords.txt`, one block a line: `# ` heading, `~ ` italic, `---` rule.
  The site says: anti-copyright, reproduce freely.
- `res/drawable-nodpi/knot_band.png` and `vigil_keepers.png` are cut from the website's
  `assets/handbillrightsideup.png`, made black on clear.
- EB Garamond, the website's typeface, from Google Fonts' repository: the variable regular and
  italic in `app/src/main/res/font/`, under the SIL Open Font License
  (`app/src/main/assets/OFL-EBGaramond.txt`). The Kompakt has no Google services, so the font
  is in the app.

No permissions. No network.

## Build

```
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
adb -d install -r app\build\outputs\apk\debug\app-debug.apk
```

Release signing uses a keystore in `signing/` (gitignored), the same pattern as Almanac; it is not
in this repository. Releases are on the GitHub releases page.
