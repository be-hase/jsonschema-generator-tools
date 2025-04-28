package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class OptionsFunctionalTest {
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
            import com.github.victools.jsonschema.generator.Option
            import com.github.victools.jsonschema.generator.SchemaVersion
            plugins {
                java
                id("dev.hsbrysk.jsonschema-generator")
            }
            jsonschemaGenerator {
                schemaVersion = SchemaVersion.DRAFT_2020_12
                options {
                    with = setOf(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                    without = setOf(Option.SCHEMA_VERSION_INDICATOR)
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
            public record Person(String name, Gender gender) {}
            """.trimIndent(),
        )
        projectDir.resolve(Path("src", "main", "java", "com", "example", "Gender.java").toFile()).writeText(
            // language=java
            """
            package com.example;
            public enum Gender {
                MALE {
                    @Override
                    public String toString() {
                        return "M";
                    }
                },
                FEMALE {
                    @Override
                    public String toString() {
                        return "F";
                    }
                },
                OTHERS {
                    @Override
                    public String toString() {
                        return "O";
                    }
                },
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
                  "type" : "object",
                  "properties" : {
                    "gender" : {
                      "type" : "string",
                      "enum" : [ "M", "F", "O" ]
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
