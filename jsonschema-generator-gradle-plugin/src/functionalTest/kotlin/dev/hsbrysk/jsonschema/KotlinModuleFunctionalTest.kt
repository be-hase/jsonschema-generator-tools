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
 * To run this test, please execute `./gradlew publishToMavenLocal` beforehand.
 */
class KotlinModuleFunctionalTest {
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
                    mavenLocal()
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun test() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                jsonschemaGenerator("dev.hsbrysk.jsonschema:jsonschema-module-kotlin:latest-SNAPSHOT")
            }
            jsonSchemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                customConfigs = mapOf(
                    "kotlin.options" to "USE_NULLABLE, USE_REQUIRED_VIA_DEFAULT_ARGS"
                )
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve(Path("src", "main", "kotlin", "com", "example").toFile()).mkdirs()
        projectDir.resolve(Path("src", "main", "kotlin", "com", "example", "Person.kt").toFile()).writeText(
            // language=kotlin
            """
            package com.example
            data class Person(val name: String = "NONAME", val age: Int, val gender: String?)
            """.trimIndent(),
        )

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("generateJsonSchema")
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
                      "type" : [ "string", "null" ]
                    },
                    "name" : {
                      "type" : "string"
                    }
                  },
                  "required" : [ "age", "gender" ]
                }
                """.trimIndent(),
            )
    }
}
