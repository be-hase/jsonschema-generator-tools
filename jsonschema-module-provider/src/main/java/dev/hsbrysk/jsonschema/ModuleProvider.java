package dev.hsbrysk.jsonschema;

import java.util.Map;

import com.github.victools.jsonschema.generator.Module;

/**
 * TODO
 */
public interface ModuleProvider {
    /**
     * TODO
     * @param properties TODO
     * @return TODO
     */
    Module provide(Map<String, String> properties);
}
