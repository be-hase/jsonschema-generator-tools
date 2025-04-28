package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class MultiProjectFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText("")
    }

    @Test
    fun `multi projects - java`() {
        settingsFile.appendText(
            // language=kotlin
            """
            include("one")
            include("two")
            include("three")
            """.trimIndent(),
        )

        projectDir.resolve(Path("one").toFile()).mkdirs()
        projectDir.resolve(Path("two").toFile()).mkdirs()
        projectDir.resolve(Path("three").toFile()).mkdirs()

        projectDir.resolve(Path("one", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            plugins {
                java
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("two", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            plugins {
                java
            }
            dependencies {
                implementation(project(":one"))
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("three", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation(project(":two"))
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("one", "src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("one", "src", "main", "java", "com", "example", "Person.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public record Person(String name, int age, String gender) {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()
        assertThat(
            projectDir.resolve(Path("three", "build", "json-schemas", "Person.json").toFile()).readText(),
        )
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "age" : {
                      "type" : "integer"
                    },
                    "gender" : {
                      "type" : "string"
                    },
                    "name" : {
                      "type" : "string"
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `multi projects - java-library`() {
        settingsFile.appendText(
            // language=kotlin
            """
            include("one")
            include("two")
            include("three")
            """.trimIndent(),
        )

        projectDir.resolve(Path("one").toFile()).mkdirs()
        projectDir.resolve(Path("two").toFile()).mkdirs()
        projectDir.resolve(Path("three").toFile()).mkdirs()

        projectDir.resolve(Path("one", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            plugins {
                `java-library`
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("two", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            plugins {
                `java-library`
            }
            dependencies {
                implementation(project(":one"))
            }
            """.trimIndent(),
        )
        projectDir.resolve(Path("three", "build.gradle.kts").toFile()).writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                `java-library`
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation(project(":two"))
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("one", "src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("one", "src", "main", "java", "com", "example", "Person.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public record Person(String name, int age, String gender) {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
            .build()
        assertThat(
            projectDir.resolve(Path("three", "build", "json-schemas", "Person.json").toFile()).readText(),
        )
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "age" : {
                      "type" : "integer"
                    },
                    "gender" : {
                      "type" : "string"
                    },
                    "name" : {
                      "type" : "string"
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
