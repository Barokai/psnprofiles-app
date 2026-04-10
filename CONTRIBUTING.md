# Contributing to PSNProfiles KMP App

Thanks for your interest in contributing! This is a fan-made hobby project, so keep expectations relaxed — PRs are welcome but may take time to review.

## Before You Start

- Check [open issues](https://github.com/Barokai/psnprofiles-app/issues) to avoid duplicate work.
- For large changes, open an issue first to discuss the approach.
- This project scrapes [PSNProfiles](https://psnprofiles.com) — contributions must not store or redistribute PSNProfiles content.

## Setting Up Locally

Requirements:

- **JDK 17** (set via your personal `~/.gradle/gradle.properties`, not committed)
- **Android Studio** (Iguana or later recommended)
- A **PSNProfiles account** for testing

```bash
git clone https://github.com/Barokai/psnprofiles-app.git
cd psnprofiles-app
./gradlew installDebug
```

> The `debug.keystore` and signing values in `gradle.properties` are intentionally committed so that sideloaded builds and GitHub artifact builds share the same signing certificate and can update each other. Do not replace the keystore.

## What to Contribute

Good candidates:

- Bug fixes for parsing / rendering issues
- UI/UX improvements (Compose-based)
- Performance improvements to HTML rendering or network layer
- iOS-side improvements (`iosApp/`)

Out of scope:

- Features that require an official PSNProfiles API that doesn't exist
- Commercial integrations
- Anything that violates PSNProfiles' Terms of Service

## Pull Request Guidelines

1. **Branch off `main`** — use a descriptive branch name, e.g. `fix/table-rendering` or `feat/lightbox-zoom`.
2. **Keep PRs focused** — one concern per PR.
3. **Test on a real device or emulator** before submitting — attach a screenshot or screen recording for UI changes.
4. **Follow the existing code style** — Kotlin official style, Compose best practices.
5. Fill in the **PR template** when opening the PR.

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) style:

```sh
feat: add zoom to lightbox
fix: table column width calculation
chore: update ktor to 2.3.13
```

## License

By contributing, you agree your changes are licensed under the project's [Apache 2.0 License](LICENSE.txt), with copyright held by Barokai.
