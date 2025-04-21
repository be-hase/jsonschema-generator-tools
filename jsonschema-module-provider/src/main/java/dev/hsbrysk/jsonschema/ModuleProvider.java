package dev.hsbrysk.jsonschema;

import java.util.Map;

public interface ModuleProvider {
    Module provide(Map<String, String> properties);
}
