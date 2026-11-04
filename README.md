# Music Stuff

A Kotlin remake of Music Stuff for Android and Windows: convert FL Studio projects and generate new songs for FL Studio Mobile.

## Features

- **Converter:** reads an FL Studio Desktop `.flp` project and writes an FL Studio Mobile `.flm` project.
- **Generator:** makes drums, bass, chords and a melody from a style, key, tempo and length. Preview the song, shuffle any part, and save it as `.flm`.

Everything runs on the device. Nothing is uploaded.

## What gets converted

- Title, tempo and time signature
- Instrument channels and their names
- Pattern notes (position, length, key, velocity, release), placed where the playlist puts them

If the playlist has no pattern clips, patterns are placed one after another.

## Limitations

- Image-Line doesn't publish the `.flm` format. The writer follows the layout of a real FL Studio Mobile 5.0.4 project, and it still needs testing in the app.
- Plugins, samples, audio clips, automation and mixer settings aren't converted. Each channel starts with an empty DirectWave sampler, so you'll need to pick instruments again in FL Studio Mobile.
- Slide notes become normal notes.
- Channels without notes are left out.

## Credits

Headings use [Lilita One](https://fonts.google.com/specimen/Lilita+One) by Juan Montoreano, under the SIL Open Font License. See `licenses/OFL-LilitaOne.txt`.

## Building

Requires JDK 17. The Android build also needs the Android SDK.

```sh
./gradlew :app:testDebugUnitTest -Pmusicstuff.modules=app
./gradlew :app:assembleDebug -Pmusicstuff.modules=app
./gradlew :desktop:run -Pmusicstuff.modules=desktop
./gradlew :desktop:packageExe -Pmusicstuff.modules=desktop
```

GitHub Actions builds on every push and uploads `Music Stuff-Android`, `Music Stuff-Windows` and rendered `Music Stuff-Screens` artifacts.
