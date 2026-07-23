# Xcas Pad (Revived)

### A Computer Algebra System for Android — brought back to life

This is a **maintained fork** of the original [Xcas Pad](https://github.com/xcaspad/android_xcaspad) app by Leonel Hernández Sandoval, which was last updated around 2018 and had accumulated numerous crashes and compatibility issues on modern Android devices.

This fork picks up where the original left off: fixing critical bugs, restoring removed features, and upgrading the app to run on current Android versions (up to Android 16 / API 36).

## What's new in this fork

- **Android 16 support** — upgraded to compileSdk/targetSdk 36, AGP 8.7.3, Gradle 8.10.2
- **Function plotting restored** — `plot(sin(x))`, `plot(x^2, x, -3, 3)` etc. render actual graphs again (was disabled in the original)
- **Critical crash fixes** — JNI memory leaks, null pointer exceptions, ClassNotFoundException on Material Components, stale RecyclerView positions
- **Session persistence** — save and restore calculator sessions
- **LaTeX export & Markdown sharing**
- **Haptic feedback** on operations

## Features

- Full Giac/Xcas CAS engine (same kernel as the HP Prime calculator)
- 2D pretty-printed math output rendered as images
- Function plotting with axes and grid
- Symbolic computation: simplify, factor, solve, integrate, differentiate, limits, series
- Numeric computation: matrices, linear algebra, statistics
- Programming: loops, functions, sequences
- Autocomplete for 400+ CAS commands
- Session history with bookmarks

## Screenshots

| Calculator | Plot |
|---|---|
| Pretty-printed CAS output | Function graphs with axes |

## Requirements

- Android 4.0.3+ (minSdk 15)
- ARM (armeabi, armeabi-v7a)

## Building from source

### Prerequisites

- Android Studio (latest)
- Android SDK with API 36
- Java 17

### Build the APK

```console
git clone https://github.com/hendr15k/android_xcaspad.git
cd android_xcaspad
./gradlew assembleRelease
```

The pre-built native library (`libxcaspad.so`) is included in `libs/`, so no NDK is required for a standard build.

### Rebuilding the native library (optional)

Only needed if you modify the JNI/C++ code:

1. Download Android NDK r10e
2. Download Giac source (1.4.9+) and place it alongside this project
3. Run `ndk-build` from the `jni/` directory

See the [original build instructions](https://github.com/xcaspad/android_xcaspad) for details.

## Credits

- **Bernard Parisse** — [Giac/Xcas](https://www-fourier.ujf-grenoble.fr/~parisse/giac.html) CAS engine
- **Leonel Hernández Sandoval** — original Xcas Pad Android app
- This fork — bug fixes, feature restoration, modern Android support

## License

GPL v3
