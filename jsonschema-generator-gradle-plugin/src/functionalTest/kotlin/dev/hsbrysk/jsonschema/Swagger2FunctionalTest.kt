package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class Swagger2FunctionalTest {
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
                implementation("io.swagger.core.v3:swagger-annotations:$SWAGGER2_VERSION")
            }
            jsonSchemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                modules {
                    swagger2Enabled = true
                }
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
            import io.swagger.v3.oas.annotations.media.Schema

            data class Person(
                @field:Schema(description = "name description")
                val name: String,
                val pet: Animal,
                @field:Schema(implementation = String::class, defaultValue = "0")
                val age: Int,
            )

            @Schema(
                subTypes = [
                    Dog::class,
                    Cat::class,
                    UMA::class,
                ],
            )
            sealed interface Animal

            data class Dog(
                val name: String,
                val breed: String,
            ) : Animal

            data class Cat(
                val name: String,
                val color: String,
            ) : Animal

            data object UMA : Animal
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
                      "type" : "string",
                      "default" : "0"
                    },
                    "name" : {
                      "type" : "string",
                      "description" : "name description"
                    },
                    "pet" : {
                      "anyOf" : [ {
                        "type" : "object",
                        "properties" : {
                          "breed" : {
                            "type" : "string"
                          },
                          "name" : {
                            "type" : "string"
                          }
                        }
                      }, {
                        "type" : "object",
                        "properties" : {
                          "color" : {
                            "type" : "string"
                          },
                          "name" : {
                            "type" : "string"
                          }
                        }
                      }, {
                        "type" : "object"
                      } ]
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
