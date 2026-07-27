package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

/**
 * Pins down the error messages users see for common misconfigurations.
 */
class FailureFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText(
            // language=kotlin
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "java", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Person.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public record Person(String name, int age, String gender) {}
            """.trimIndent(),
        )
    }

    @Test
    fun `schema target class not found`() {
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
                schemas {
                    create("Missing") {
                        target = "com.example.Missing"
                    }
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema", "--configuration-cache")
            .buildAndFail()

        assertThat(result.task(":generateJsonSchema")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("com.example.Missing")
    }

    @Test
    fun `type mapping class not found`() {
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
                    "java.time.Duration" to "com.example.DoesNotExist",
                )
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema", "--configuration-cache")
            .buildAndFail()

        assertThat(result.task(":generateJsonSchema")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("com.example.DoesNotExist")
    }

    @Test
    fun `upload without bucket`() {
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
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("uploadJsonSchemaToS3", "--configuration-cache")
            .buildAndFail()

        // Schema generation itself succeeds; only the upload task fails on the missing bucket
        assertThat(result.task(":generateJsonSchema")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":uploadJsonSchemaToS3")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("bucket")
    }
}
