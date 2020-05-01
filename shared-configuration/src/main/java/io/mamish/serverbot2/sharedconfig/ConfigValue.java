package io.mamish.serverbot2.sharedconfig;

import java.util.function.Function;

/**
 * A configuration value that is lazy-loaded when first used.
 * Intended for dynamic values that needs to be fetched (e.g. secrets and parameters).
 */
public class ConfigValue {

    private String name;
    private String value;
    private Function<String,String> fetcher;

    public ConfigValue(String name, Function<String,String> fetcher) {
        this.name = name;
        this.fetcher = fetcher;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        if (value == null) {
            synchronized (this) {
                // Check null a second time, in case there was lock contention and another thread already ran first
                if (value == null) {
                    value = fetcher.apply(name);
                }
            }
        }
        return value;
    }

}
