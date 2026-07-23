package dev.hsbrysk.jsonschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class SchemaPropertyFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @BeforeEach
    fun beforeEach() {
        settingsFile.writeText("")

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
    fun enabled() {
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
                schemaProperty {
                    enabled = true
                }
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
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

    @Test
    fun `enabled via property syntax`() {
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
                schemaProperty.enabled.set(true)
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
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

    @Test
    fun `draft7 with definitionForMainSchema`() {
        writeBuildFileWithOptions(
            schemaVersion = "DRAFT_7",
            options = "Option.DEFINITION_FOR_MAIN_SCHEMA",
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
                  "${'$'}schema" : "http://json-schema.org/draft-07/schema#",
                  "${'$'}ref" : "#/definitions/Person",
                  "definitions" : {
                    "Person" : {
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
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `draft2020 with definitionForMainSchema`() {
        writeBuildFileWithOptions(
            schemaVersion = "DRAFT_2020_12",
            options = "Option.DEFINITION_FOR_MAIN_SCHEMA",
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
                  "${'$'}ref" : "#/${'$'}defs/Person",
                  "${'$'}defs" : {
                    "Person" : {
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
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `draft7 with definitionForMainSchema and forbiddenAdditionalProperties`() {
        writeBuildFileWithOptions(
            schemaVersion = "DRAFT_7",
            options = "Option.DEFINITION_FOR_MAIN_SCHEMA, Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT",
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
                  "${'$'}schema" : "http://json-schema.org/draft-07/schema#",
                  "${'$'}ref" : "#/definitions/Person",
                  "definitions" : {
                    "Person" : {
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
                      "additionalProperties" : false,
                      "required" : [ "${'$'}schema" ]
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    @Test
    fun `draft2020 with definitionForMainSchema and forbiddenAdditionalProperties`() {
        writeBuildFileWithOptions(
            schemaVersion = "DRAFT_2020_12",
            options = "Option.DEFINITION_FOR_MAIN_SCHEMA, Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT",
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
                  "${'$'}ref" : "#/${'$'}defs/Person",
                  "${'$'}defs" : {
                    "Person" : {
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
                      "additionalProperties" : false,
                      "required" : [ "${'$'}schema" ]
                    }
                  }
                }
                """.trimIndent(),
            )
    }

    private fun writeBuildFileWithOptions(
        schemaVersion: String,
        options: String,
    ) {
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
                schemaVersion = SchemaVersion.$schemaVersion
                options {
                    with = setOf($options)
                }
                schemaProperty {
                    enabled = true
                }
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `enabled but not required`() {
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
                schemaProperty {
                    enabled = true
                    required = false
                }
                schemas {
                    create("Person") {
                        target = "com.example.Person"
                    }
                }
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
                  }
                }
                """.trimIndent(),
            )
    }
}
