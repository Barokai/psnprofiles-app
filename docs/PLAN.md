# Toolchain upgrade plan

_Written 2026-11-27._

## Problem

All open Renovate PRs fail "Build APK"; `main` was last green on 2026-04-10. Each one fails for its own reason,
but they share a root cause: **the toolchain is too old for current dependencies.**

| Current | Value |
|---|---|
| Kotlin (`kotlin.version`) | 1.9.21 (Dec 2023) |
| Android Gradle Plugin (`agp.version`) | 8.9.0 |
| Compose Multiplatform (`compose.version`) | 1.5.11 |
| Gradle wrapper | 8.14.4 |
| JDK in CI | 17 (`actions/setup-java@v4`) |
| compile/target SDK | 36, minSdk 24 |

What the failing PRs show:

| PR | Update | Why it fails |
|---|---|---|
| #24 | AndroidX | `androidx.core:core-ktx:1.19.1` needs **AGP ≥ 9.1.0**; `navigationevent` needs AGP ≥ 8.9.1 |
| #25 | kotlinx-coroutines 1.11 | probably needs a newer Kotlin compiler (metadata version) |
| #26 | multiplatform-settings 1.3 | same: probably needs newer Kotlin |
| #27 | GitHub Actions majors | the new `setup-java` major brings a newer JDK, and Kotlin 1.9.21 crashes while parsing its version string (`JavaVersion.parse("25.0.4.1")`) |
| #28 | Gradle 9 | KGP 1.9.21 uses `DefaultArtifactPublicationSet`, an internal Gradle class removed in Gradle 9 |

The build also warns that AGP 8.9.0 is "higher than the maximum known to the Kotlin Gradle Plugin", and that the
`getting` delegates in `androidApp/build.gradle.kts` and `shared/build.gradle.kts` are deprecated in Gradle 9.6.

## Plan

One branch that lifts the whole toolchain together; the Renovate PRs follow afterwards.

1. **Kotlin 2.x** (current 2.3 line) with the **Compose compiler Gradle plugin** (`org.jetbrains.kotlin.plugin.compose`)
   - required since Kotlin 2.0, so the Compose compiler is no longer configured through Compose Multiplatform
2. **Compose Multiplatform** to a version that matches that Kotlin
3. **AGP 9.x** (≥ 9.1 for current AndroidX). With AGP 9:
   - Kotlin Multiplatform modules move to the new `com.android.kotlin.multiplatform.library` plugin instead of
     `com.android.library` + `androidTarget()`
   - check the AGP 9 migration guide for removed DSL
4. **Gradle 9.x** wrapper (whatever AGP 9.x requires); replace the `by getting { }` source-set delegates with
   `getByName(...)` / the `named()` API
5. **JDK:** pin the toolchain explicitly (`kotlin { jvmToolchain(21) }` or 17), so a newer JDK on the CI image
   doesn't change the build. Use the same in `setup-java`.
6. **Version catalog:** move `kotlin.version` / `agp.version` / `compose.version` from `gradle.properties` and
   `settings.gradle.kts` into `gradle/libs.versions.toml`, so Renovate updates them as one group
7. **ksoup:** the Renovate rule pins it to `<0.2.0` because 0.2.x needs Kotlin 2.3+. Remove the pin after step 1.
8. Build locally (`./gradlew :androidApp:assembleDebug`) and on CI, then smoke-test the APK on a device
9. Then let Renovate rebase #24–#28 (several will be obsolete) and merge the green ones

## Done when
- `main` is green with Kotlin 2.x, AGP 9.x, Gradle 9.x and a pinned JDK
- No Renovate PR is blocked by the toolchain any more, and the ksoup pin is gone
