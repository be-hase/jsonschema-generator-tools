package com.example

import com.github.victools.jsonschema.generator.Module
import dev.hsbrysk.jsonschema.ModuleProvider

class SecretStringModuleProvider : ModuleProvider {
    override fun provide(properties: Map<String, String?>): Module = SecretStringModule()
}
