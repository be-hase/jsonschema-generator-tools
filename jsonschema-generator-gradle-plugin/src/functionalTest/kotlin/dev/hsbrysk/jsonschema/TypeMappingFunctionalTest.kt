package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class TypeMappingFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText("")
    }

    @Test
    fun test() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                typeMappings = mapOf(
                    "java.time.Duration" to "java.lang.Integer",
                )
                schemas {
                    create("Options") {
                        target = "com.example.Options"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Options.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import java.time.Duration;
            import java.util.List;
            public record Options(Duration timeout, List<Duration> timeoutList) {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema", "--configuration-cache")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Options.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "timeout" : {
                      "type" : "integer"
                    },
                    "timeoutList" : {
                      "type" : "array",
                      "items" : {
                        "type" : "integer"
                      }
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `subtype matching and precedence`() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                typeMappings = mapOf(
                    "java.time.Duration" to "java.lang.Integer",
                    // Duration also implements TemporalAmount, but the first matching mapping wins
                    "java.time.temporal.TemporalAmount" to "java.lang.String",
                )
                schemas {
                    create("Times") {
                        target = "com.example.Times"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Times.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            import java.time.Duration;
            import java.time.Period;
            public record Times(Duration timeout, Period period) {}
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema", "--configuration-cache")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Times.json").toFile()).readText())
            .isEqualTo(
                // language=json
                """
                {
                  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "period" : {
                      "type" : "string"
                    },
                    "timeout" : {
                      "type" : "integer"
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
