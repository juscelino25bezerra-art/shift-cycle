# Third-party components — shift-cycle

shift-cycle is licensed under the **GPL-3.0-or-later** ([LICENSE](LICENSE)), Copyright (C) 2026 Juscelino Be. Its source code is original: it was written for the author's apps (the Minha Escala PWA and Kalendrium) and contains no code copied from other projects.

## Runtime dependency (not bundled)

The library ships only its own classes. Projects that use it also get this dependency from Maven Central:

| Component | Version | Source | License (SPDX) | Copyright / notice |
|---|---|---|---|---|
| Kotlin standard library (`org.jetbrains.kotlin:kotlin-stdlib`) and `org.jetbrains:annotations` | 2.1.0 / 13.0 | https://github.com/JetBrains/kotlin | Apache-2.0 | Copyright 2010-2024 JetBrains s.r.o and respective authors and developers (NOTICE in the Kotlin repository) |

The Apache License 2.0 is compatible with the GPL-3.0. An app that bundles shift-cycle also bundles the Kotlin standard library and must keep its license and NOTICE.

`java.time` comes from the Java platform (or from Android's core library desugaring on old devices) and is not a dependency of this library.

## Tests only (not distributed)

| Component | License |
|---|---|
| Kotlin Test (`kotlin-test`, `kotlin-test-junit5`) | Apache-2.0 |
| JUnit 5 (Jupiter, Platform), pulled in by `kotlin-test-junit5` | EPL-2.0 |

## Build tools (not distributed)

Gradle 8.13 and its wrapper (Apache-2.0), Kotlin Gradle plugin 2.1.0 (Apache-2.0), and GitHub Actions in CI: `actions/checkout`, `actions/setup-java` (MIT) and `gradle/actions` (MIT).

## Reporting a license problem

Open an issue whose title starts with **[license]**, naming the file or dependency and the source of the information.
