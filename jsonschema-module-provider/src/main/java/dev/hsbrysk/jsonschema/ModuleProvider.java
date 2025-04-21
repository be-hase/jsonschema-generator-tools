package dev.hsbrysk.jsonschema;

import java.util.Map;

import com.github.victools.jsonschema.generator.Module;

/**
 * A class used for advanced and flexible configuration with the jsonschema-generator-gradle-plugin
 */
public interface ModuleProvider {
    /**
     * @param properties Gradle properties matching `jsonschema.generator.*` will be passed in.
     *   By using these, you can generate a module with configurable settings.
     * @return Module to be applied to the jsonschema-generator
     */
    Module provide(Map<String, String> properties);
}
