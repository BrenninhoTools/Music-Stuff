# Music Stuff

A Kotlin remake of Music Stuff for Android: tools for moving FL Studio projects between desktop and mobile.

## Features

- **FLP to FLM converter:** reads an FL Studio Desktop `.flp` project and writes an FL Studio Mobile `.flm` project.

Everything runs on the device. Nothing is uploaded.

## What gets converted

- Title, tempo and time signature
- Instrument channels and their names
- Pattern notes (position, length, key, velocity, release, slide), placed where the playlist puts them

If the playlist has no pattern clips, patterns are placed one after another.

## Limitations

- Image-Line doesn't publish the `.flm` format. The writer is based on reverse-engineering, and it still needs testing in FL Studio Mobile.
- Plugins, samples, audio clips, automation and mixer settings aren't converted. You'll need to pick instruments again in FL Studio Mobile.
- `.flm` notes can start up to beat 1023. Anything later is skipped, and the app tells you how many notes were affected.

## Building

Requires JDK 17 and the Android SDK.

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

GitHub Actions builds the debug APK on every push and uploads it as the `Music Stuff-Android` artifact.
