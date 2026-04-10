# PSNProfiles KMP Application
[![Android Build & Release](https://github.com/Barokai/psnprofiles-app/actions/workflows/android_build.yml/badge.svg)](https://github.com/Barokai/psnprofiles-app/actions/workflows/android_build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A high-performance, native [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) application for browsing **PSNProfiles** trophy guides with a premium, mobile-optimized experience.

---

## 🌟 Key Features

- **Consolidated Trophy Guides**: Fragmented guide boxes are merged into single, cohesive trophy cards for a cleaner layout.
- **Native Table Rendering**: Advanced HTML-to-Compose table engine with "Weight-First" layout to ensure images and text are perfectly balanced.
- **Fullscreen Image Lightbox**: Dynamic tap-to-fullscreen support with landscape orientation for high-detail screenshots.
- **High-Fidelity UI**: Modern, dark-mode-first aesthetic with Inter typography and vibrant accent colors.
- **Fast Navigation**: Optimized Table of Contents and Roadmap deep-linking with recursive anchor mapping.

---

## 🛠 Build & Deployment

### Local Development
To run the project locally on your Android device or emulator:
```bash
./gradlew installDebug
```

> **Note for developers**: Do **not** commit `org.gradle.java.home` to `gradle.properties`.
> If you need to pin a specific local JDK, set it in your personal `~/.gradle/gradle.properties` file instead:
> ```properties
> org.gradle.java.home=/path/to/your/local/jdk
> ```
> This keeps machine-specific paths out of version control and prevents CI failures.

### CI/CD Pipeline
- **Automated Builds**: Every push to `main` and all Pull Requests automatically build a debug APK.
- **GitHub Artifacts**: Download the latest build from the "Actions" tab.
- **Manual Releases**: To create a formal release with a changelog:
  1. Go to the **Actions** tab in GitHub.
  2. Select **Android Build & Release**.
  3. Click **Run workflow**, check **Create Release**, and enter a version number (e.g., `1.0.1`).

> **Note on committed debug keystore**: `androidApp/debug.keystore` and the `signing.*` values in `gradle.properties` are **intentionally committed**. This ensures that a locally ADB-sideloaded build and a downloaded GitHub artifact share the same signing certificate, so Android treats them as the same app and allows seamless updates between the two — no reinstall required. The credentials are the standard Android debug defaults (`android` / `androiddebugkey`) and are not production secrets. A future improvement may add APK naming prefixes (e.g. `-debug.apk`) to make artifact identification clearer.

---

## 📸 Screenshots

| Trophy Guide Grid | Fullscreen Lightbox | Balanced Table Rendering |
| :---: | :---: | :---: |
| ![Guide Grid](https://github.com/Barokai/psnprofiles-app/raw/main/readme_images/guide_grid.png) | ![Lightbox](https://github.com/Barokai/psnprofiles-app/raw/main/readme_images/lightbox.png) | ![Tables](https://github.com/Barokai/psnprofiles-app/raw/main/readme_images/tables.png) |

---

## 🏛 Architecture

- **shared**: Contains the core parsing logic (`GuideRepository`), networking, and the `NativeHtmlRenderer` component.
- **androidApp**: Android-specific entry point and UI hosting.
- **compose**: Built with Jetpack Compose (Multiplatform) for a truly native feel.

---

## 🤖 Acknowledgements

The majority of the implementation in this project was built with the assistance of AI models (primarily **Google Antigravity**), and a healthy dose of [GitHub Copilot](https://github.com/features/copilot) — the fine art of making things work through determined searching, caffeine, and trial and error. The architecture decisions, product direction, and all the glue holding it together are human-made.

---

## ⚠️ Disclaimer

- **Fan project**: This app is an independent, fan-made tool and is **not affiliated with, endorsed by, or in any way officially connected to PSNProfiles** or Sony Interactive Entertainment.
- **No official API**: The app retrieves data by parsing PSNProfiles web pages directly. There is no official PSNProfiles API in use. Users authenticate using their own session — no credentials are stored by this project.
- **Terms of Service**: PSNProfiles' Terms of Service may restrict automated access to their platform. Use this app at your own risk. The authors accept no liability for account restrictions or other consequences arising from its use.
- **All trademarks** (PlayStation, PSN, trophy icons, etc.) belong to their respective owners.

### For Testers

- Requires an active [PSNProfiles](https://psnprofiles.com) account and an internet connection.
- Only **debug APKs** are produced — there is no production signing configuration and the app is not distributed via the Google Play Store.
- Install via ADB (`./gradlew installDebug`) or by sideloading a downloaded GitHub artifact. Both use the same shared debug signing certificate and can update each other without reinstalling.
- There is no automatic update mechanism — check the Actions tab for new builds.

---

## ⚖ License

Licensed under the Apache License, Version 2.0. See [LICENSE.txt](LICENSE.txt) for details.