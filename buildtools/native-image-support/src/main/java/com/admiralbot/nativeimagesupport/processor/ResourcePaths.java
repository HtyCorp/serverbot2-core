package com.admiralbot.nativeimagesupport.processor;

public enum ResourcePaths {

    GSON_ADAPTERS_RESOURCE("META-INF/admiralbot/gson-adapters-init.list"),
    API_DEFINITION_SETS_RESOURCE("META-INF/admiralbot/api-deinition-sets-init.list"),
    TABLE_SCHEMAS_RESOURCE("META-INF/admiralbot/table-schemas-init.list");

    private final String path;

    ResourcePaths(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
