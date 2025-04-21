package com.example

import com.github.victools.jsonschema.generator.Module
import dev.hsbrysk.jsonschema.ModuleProvider

class SecretStringModuleProvider : ModuleProvider {
    override fun provide(customConfigs: Map<String, String?>): Module = SecretStringModule()
}
