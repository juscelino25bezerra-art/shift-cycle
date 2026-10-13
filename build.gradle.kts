// shift-cycle — Copyright (C) 2026 Juscelino Be
// SPDX-License-Identifier: GPL-3.0-or-later

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    `java-library`
    `maven-publish`
}

group = "io.github.juscelino25bezerra-art"
version = "1.0.0"

java {
    // runs on any JVM 11+ and on Android (java.time: API 26+, or core library desugaring below that)
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
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

// the license travels with every jar (classes and sources)
tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { into("META-INF") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "shift-cycle"
            pom {
                name.set("shift-cycle")
                description.set("Kotlin engine for rotating work schedules: combined blocks, date changes and cycle edits that keep past days intact.")
                url.set("https://github.com/juscelino25bezerra-art/shift-cycle")
                licenses {
                    license {
                        name.set("GPL-3.0-or-later")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("juscelino25bezerra-art")
                        name.set("Juscelino Be")
                        url.set("https://github.com/juscelino25bezerra-art")
                    }
                }
                scm {
                    url.set("https://github.com/juscelino25bezerra-art/shift-cycle")
                    connection.set("scm:git:https://github.com/juscelino25bezerra-art/shift-cycle.git")
                }
            }
        }
    }
}
