# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and verification

This is currently a Gradle Kotlin/JVM project with one module, `:dsp`. Run commands from the repository root using the checked-in Gradle Wrapper. On Windows:

```powershell
.\gradlew.bat :dsp:build
.\gradlew.bat :dsp:test
.\gradlew.bat :dsp:verifyReport
```

- `:dsp:build` compiles the module and runs its standard checks.
- `:dsp:test` runs the JUnit Platform test suite.
- `:dsp:verifyReport` runs the M1 synthetic-sine acceptance report and prints per-note YIN error details.

Run one test class or one test method with Gradle's test filter. Quote method names because Kotlin test names contain spaces:

```powershell
.\gradlew.bat :dsp:test --tests "com.example.guitartuner.PitchDetectorTest"
.\gradlew.bat :dsp:test --tests "com.example.guitartuner.PitchDetectorTest.detects frequency offset of 3 cents sharp"
```

There is currently no Android lint, ktlint, detekt, or other lint task configured. The Android application and its run/install commands do not exist yet.

## Toolchain and project state

- The root build declares Kotlin JVM `2.0.21`.
- The checked-in Gradle Wrapper uses Gradle `8.10.2`.
- `:dsp` requests JVM toolchain `21`.
- Tests use Kotlin Test with JUnit Jupiter `5.10.2` on the JUnit Platform.
- The implemented M1 code is pure Kotlin/JVM and has no Android dependency. M1 builds and tests require a JDK, not the Android SDK or a device.
- The design documents describe an Android/Compose application for later milestones, but there is currently no `:app` module. Android SDK, microphone permissions, `AudioRecord`, and real-device validation belong to M2 and later.
- Dependency repositories are centralized in `settings.gradle.kts`, with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`; add or change repositories there rather than in a module build script.

## Architecture

The intended tuner pipeline is:

`AudioRecord PCM capture and frame preprocessing` → `PitchDetector.detect` → `NoteMapper` against the selected `Tuning` → `cents`-driven UI state.

Only the platform-independent DSP and music-domain core exists today:

- `PitchDetector.detect` accepts one `ShortArray` PCM frame and returns a nullable fundamental frequency. It implements YIN's difference function, cumulative mean normalization, threshold/local-minimum selection, and parabolic interpolation. It does not choose a target string or know about Android or UI state. `null` means that no pitch candidate passed the detector's search threshold.
- `TuningNote` stores a note name and MIDI number. Its target frequency is derived through `NoteMapper.midiToFreq`, rather than stored as a second hand-maintained constant.
- `NoteMapper.freqToNote` maps a detected frequency to the nearest equal-tempered note and octave. `NoteMapper.cents` compares the detected frequency with the selected `TuningNote.frequency`; positive values are sharp and negative values are flat.
- `Tunings.ALL` is the source of the built-in targets: six tunings in total, with three six-string guitar tunings and three four-string ukulele tunings, for 30 tuning-note entries. The current standard ukulele definition uses high-G re-entrant tuning.
- The acceptance path generates 44.1 kHz, 2048-sample synthetic sine-wave PCM frames, sends them through the detector and note mapping, and checks all entries in `Tunings.ALL` to within 1 cent. This validates the clean-signal JVM core, not real microphone behavior.

Keep platform-specific concerns out of `:dsp`. When the Android module is added, it should depend on and reuse this module while keeping microphone capture, permissions, frame preprocessing, threading/lifecycle, smoothing, and Compose presentation in the application layer.

## Milestone boundary

M1 is the reusable JVM DSP core. M2 and later extend the product with Android audio capture and UI; do not add Android dependencies to the existing DSP module merely to implement those layers. The design reserves Oboe/AAudio as a later real-device optimization only if measured end-to-end latency exceeds the project's target or the indicator visibly lags; it is not part of the current implementation.
