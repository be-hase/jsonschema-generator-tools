package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

/**
 * All other functional tests use the Kotlin DSL. Groovy resolves extension properties
 * differently, so make sure the plugin DSL also works from a `build.gradle`.
 */
class GroovyDslFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText(
            // language=groovy
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun groovy() {
        buildFile.writeText(
            // language=groovy
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                id 'java'
                id 'dev.hsbrysk.jsonschema-generator'
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                schemaProperty {
                    enabled = true
                }
                schemas {
                    Person {
                        target = 'com.example.Person'
                    }
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

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema", "--configuration-cache")
            .build()

        assertThat(projectDir.resolve(Path("build", "json-schemas", "Person.json").toFile()).readText())
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
                    },
                    "${'$'}schema" : {
                      "type" : "string"
                    }
                  },
                  "required" : [ "${'$'}schema" ]
                }
                """.trimIndent(),
            )
    }
}
