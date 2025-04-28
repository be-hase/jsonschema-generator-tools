package dev.hsbrysk.jsonschema.module.kotlin

import com.github.victools.jsonschema.generator.Module
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.kotlinProperty

class KotlinModule(vararg val options: KotlinOption) : Module {
    constructor(options: Set<KotlinOption>) : this(*options.toTypedArray())

    override fun applyToConfigBuilder(builder: SchemaGeneratorConfigBuilder) {
        if (options.contains(KotlinOption.USE_NULLABLE)) {
            builder.forFields().withNullableCheck { field ->
                checkNotNull(field.rawMember.kotlinProperty).returnType.isMarkedNullable
            }
        }

        if (options.contains(KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS)) {
            builder.forFields().withRequiredCheck { field ->
                val primaryConstructor =
                    checkNotNull(field.rawMember.declaringClass.kotlin.primaryConstructor)
                val param = primaryConstructor.parameters.firstOrNull { field.name == it.name }
                if (param == null) {
                    false
                } else {
                    !param.isOptional
                }
            }
        }
    }
}
