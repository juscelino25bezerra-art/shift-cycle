// shift-cycle — Copyright (C) 2026 Juscelino Be
// SPDX-License-Identifier: GPL-3.0-or-later

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    `java-library`
}

group = "io.github.juscelino25bezerra-art"
version = "1.0.0"

java {
    // runs on any JVM 11+ and on Android (java.time: API 26+, or core library desugaring below that)
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // no runtime dependencies besides the Kotlin standard library (added by the Kotlin plugin)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
