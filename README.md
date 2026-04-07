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

### CI/CD Pipeline
- **Automated Builds**: Every push to `main` and all Pull Requests automatically build a debug APK.
- **GitHub Artifacts**: Download the latest build from the "Actions" tab.
- **Manual Releases**: To create a formal release with a changelog:
  1. Go to the **Actions** tab in GitHub.
  2. Select **Android Build & Release**.
  3. Click **Run workflow**, check **Create Release**, and enter a version number (e.g., `1.0.1`).

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

## ⚖ License
Licensed under the Apache License, Version 2.0. See [LICENSE.txt](LICENSE.txt) for details.