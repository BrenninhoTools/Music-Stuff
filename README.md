# Music Stuff

A Kotlin remake of Music Stuff for Android: tools for moving FL Studio projects between desktop and mobile.

## Features

- **FLP to FLM converter:** reads an FL Studio Desktop `.flp` project and writes an FL Studio Mobile `.flm` project.

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

Requires JDK 17 and the Android SDK.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

GitHub Actions builds the debug APK on every push and uploads it as the `Music Stuff-Android` artifact.
