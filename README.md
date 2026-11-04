# Music Stuff

A Kotlin remake of Music Stuff for Android, iOS, Windows, Linux and macOS: convert FL Studio projects and generate new songs for FL Studio Mobile.

## Features

- **Converter:** turns FL Studio Desktop `.flp` projects into FL Studio Mobile `.flm` projects, and `.flm` back into `.flp`.
- **Generator:** makes drums, bass, chords and a melody from a style, key, tempo and length. Preview the song, shuffle any part, and save it as `.flm`.

Everything runs on the device. Nothing is uploaded.

## What gets converted

- Title, tempo and time signature
- Instrument channels and their names
- Pattern notes (position, length, key, velocity, release), placed where the playlist puts them

If the playlist has no pattern clips, patterns are placed one after another.

## Limitations

- Image-Line doesn't publish these formats. The `.flm` code follows a real FL Studio Mobile 5.0.4 project and the `.flp` writer follows real FL Studio 20 projects, and both still need testing in the apps.
- Plugins, samples, audio clips, automation and mixer settings aren't converted. Each channel starts with an empty DirectWave sampler, so you'll need to pick instruments again in FL Studio Mobile.
- Slide notes become normal notes.
- Channels without notes are left out.
- FLM to FLP turns each clip into its own pattern, and FL Studio opens the project with empty Sampler channels and a default mixer.

## Credits

Headings use [Lilita One](https://fonts.google.com/specimen/Lilita+One) by Juan Montoreano, under the SIL Open Font License. See `licenses/OFL-LilitaOne.txt`.

## Building

Requires JDK 17. The Android build also needs the Android SDK.

```sh
./gradlew :app:testDebugUnitTest -Pmusicstuff.modules=app
./gradlew :app:assembleDebug -Pmusicstuff.modules=app
./gradlew :desktop:run -Pmusicstuff.modules=desktop
./gradlew :desktop:packageExe -Pmusicstuff.modules=desktop
./gradlew :desktop:packageDeb :desktop:createDistributable -Pmusicstuff.modules=desktop
./gradlew :desktop:packageDmg :desktop:createDistributable -Pmusicstuff.modules=desktop
./gradlew :ios:linkDebugFrameworkIosArm64 -Pmusicstuff.modules=ios
```

The iOS app needs macOS, Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen). After linking the framework, run `xcodegen generate` in `iosApp`. The `.ipa` from CI is unsigned, so sign it or sideload it to install.

GitHub Actions builds on every push and uploads `Music Stuff-Android`, `Music Stuff-Windows`, `Music Stuff-Linux`, `Music Stuff-MacOS`, `Music Stuff-IOS` and rendered `Music Stuff-Screens` artifacts.
