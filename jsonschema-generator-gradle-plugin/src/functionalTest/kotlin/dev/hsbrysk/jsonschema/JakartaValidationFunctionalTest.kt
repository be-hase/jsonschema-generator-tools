package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class JakartaValidationFunctionalTest {
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
    fun java() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("jakarta.validation:jakarta.validation-api:$JAKARTA_VALIDATION_VERSION")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                modules {
                    jakartaValidationEnabled = true
                }
                schemas {
                    create("Person") {
                        target = "com.example.Person"
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
            import jakarta.validation.constraints.Max;
            import jakarta.validation.constraints.Min;
            import jakarta.validation.constraints.NotBlank;
            public record Person(
                    @NotBlank String name,
                    @Min(0) @Max(200) int age
            ) {
            }
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
                      "type" : "integer",
                      "minimum" : 0,
                      "maximum" : 200
                    },
                    "name" : {
                      "type" : "string",
                      "minLength" : 1
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun kotlin() {
        buildFile.writeText(
            // language=kotlin
            """
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                kotlin("jvm") version "$KOTLIN_VERSION"
                id("dev.hsbrysk.jsonschema-generator")
            }
            dependencies {
                implementation("jakarta.validation:jakarta.validation-api:$JAKARTA_VALIDATION_VERSION")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                modules {
                    jakartaValidationEnabled = true
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
            import jakarta.validation.constraints.Max
            import jakarta.validation.constraints.Min
            import jakarta.validation.constraints.NotBlank
            data class Person(
                @field:NotBlank
                val name: String,

                @field:Min(0)
                @field:Max(200)
                val age: Int,
            )
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
                      "type" : "integer",
                      "minimum" : 0,
                      "maximum" : 200
                    },
                    "name" : {
                      "type" : "string",
                      "minLength" : 1
                    }
                  }
                }
                """.trimIndent(),
            )
    }
}
