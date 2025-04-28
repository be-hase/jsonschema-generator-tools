package dev.hsbrysk.jsonschema.module.kotlin

import com.github.victools.jsonschema.generator.Module
import dev.hsbrysk.jsonschema.ModuleProvider

class KotlinModuleProvider : ModuleProvider {
    override fun provide(customConfigs: Map<String, String>): Module {
        val options = customConfigs["kotlin.options"]
            ?.split(",")
            ?.map { it.trim() }
            ?.map { KotlinOption.valueOf(it) }
            ?.toSet()
            ?: emptySet()
        return KotlinModule(options)
    }
}
