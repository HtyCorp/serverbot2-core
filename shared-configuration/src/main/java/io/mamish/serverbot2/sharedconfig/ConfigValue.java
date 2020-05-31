package io.mamish.serverbot2.sharedconfig;

import java.util.function.Function;

/**
 * A configuration value that is lazy-loaded when first used.
 * Intended for dynamic values that needs to be fetched (e.g. secrets and parameters).
 */
public class ConfigValue {

    private String name;
    private volatile boolean hasBeenSet = false;
    private volatile String value;
    private Function<String,String> fetcher;

    public ConfigValue(String name, Function<String,String> fetcher) {
        this.name = name;
        this.fetcher = fetcher;
    }

    public String getName() {
        return name;
    }

    public boolean notNull() {
        ensureInitialised();
        return value != null;
    }

    public String getValue() {
        ensureInitialised();
        return value;
    }

    private void ensureInitialised() {
        // Using an explicit has-been-set var (instead of null check) because null is occasionally a final value
        if (!hasBeenSet) {
            synchronized (this) {
                // Check null a second time, in case there was lock contention and another thread already ran first
                if (!hasBeenSet) {
                    value = fetcher.apply(name);
                    hasBeenSet = true;
                }
            }
        }
    }

}
