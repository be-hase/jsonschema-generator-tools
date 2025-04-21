package com.example

import com.github.victools.jsonschema.generator.Module
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder

class SecretStringModule : Module {
    override fun applyToConfigBuilder(builder: SchemaGeneratorConfigBuilder) {
        builder.forFields().withTargetTypeOverridesResolver { field ->
            if (SecretString::class.java.isAssignableFrom(field.rawMember.type)) {
                listOf(field.context.resolve(String::class.java))
            } else {
                null
            }
        }
    }
}
