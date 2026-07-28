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
                // Returning null leaves the decision to the default handling,
                // so fields of non-Kotlin classes are unaffected.
                field.rawMember.kotlinProperty?.returnType?.isMarkedNullable
            }
        }

        if (options.contains(KotlinOption.USE_REQUIRED_VIA_DEFAULT_ARGS)) {
            builder.forFields().withRequiredCheck { field ->
                // Non-Kotlin classes and classes without a primary constructor have no
                // default-argument information, so nothing is marked as required.
                val primaryConstructor = field.rawMember.declaringClass.kotlin.primaryConstructor
                val param = primaryConstructor?.parameters?.firstOrNull { field.name == it.name }
                param != null && !param.isOptional
            }
        }
    }
}
