package dev.hsbrysk.jsonschema.module.kotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import org.junit.jupiter.api.Test

class KotlinModuleTest {
    @Test
    fun useNullable() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_NULLABLE))
                .build(),
        )
        assertThat(generator.generateSchema(Person::class.java).toPrettyString()).isEqualTo(
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
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun useRequiredViaDefaultArgs() {
        val generator = SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(KotlinModule(KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS))
                .build(),
        )
        assertThat(generator.generateSchema(Person::class.java).toPrettyString()).isEqualTo(
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
              },
              "required" : [ "age", "gender" ]
            }
            """.trimIndent(),
        )
    }
}

data class Person(val name: String = "NONAME", val age: Int, val gender: String?)
