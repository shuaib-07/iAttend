# iAttend

A free, offline-first attendance and timetable tracker for Android. Built with Jetpack Compose, Room, and Hilt.

## Features

- **Attendance tracking** — mark present, absent, or cancelled per class
- **Flexible timetable** — different subjects and timings, any day
- **Recurring holidays** — set your weekly off-days once
- **Extra classes** — schedule one-off sessions anytime
- **Multiple timetable versions** — switch schedules mid-term without losing your class count
- **Exams & tests** — track dates, marks, and portions, with their own reminders
- **Smart reminders** — before class starts, right after it ends, and before tests/exams
- **Bunk calculator** — see exactly how many classes you can afford to skip
- **Themes** — light, dark, AMOLED, and multiple accent colors, with Material You dynamic color
- **Home screen widget** — glance at upcoming classes without opening the app
- **Share your timetable** — export/import as a portable backup file
- **Fully offline & private** — no account, no analytics, nothing leaves your device

## Building

```
git clone <this-repo>
cd iAttend
./gradlew assembleDebug
```

Requires JDK 11+ and the Android SDK (compileSdk 36, minSdk 24). A release build needs `app/keystore.properties` pointing at a signing key — see `app/build.gradle.kts` for the expected format; debug builds work out of the box.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.

## Privacy

iAttend collects nothing. See [PRIVACY.md](PRIVACY.md) for the full statement.

## License

Licensed under either of [Apache License, Version 2.0](LICENSE-APACHE) or [MIT license](LICENSE-MIT) at your option. Contributions are accepted under the same dual-license terms unless you state otherwise.
