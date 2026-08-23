# Contributing to iAttend

Thanks for considering a contribution.

## Getting set up

1. Fork and clone the repo.
2. Open in Android Studio (or run `./gradlew assembleDebug` from the CLI) — JDK 11+, Android SDK with compileSdk 36.
3. Debug builds work with no extra setup. A release build needs `app/keystore.properties` (see `app/build.gradle.kts`); you don't need this for local development.

## Making changes

- Keep PRs focused — one feature or fix per PR is easier to review than a bundle of unrelated changes.
- Match the existing code style: Jetpack Compose + Material3, MVVM with Hilt-injected ViewModels, Room for persistence, `StateFlow` for UI state.
- If you touch the database schema, add a Room `Migration` — don't rely on `fallbackToDestructiveMigration`.
- Test on both light and dark theme if your change touches UI.

## Submitting

1. Open a PR against `main` with a clear description of what changed and why.
2. Link any related issue.
3. Be ready for review feedback — this is a small project, reviews may take a bit.

## Reporting bugs / requesting features

Use the issue templates under `.github/ISSUE_TEMPLATE/`.

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
