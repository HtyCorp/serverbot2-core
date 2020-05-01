package io.mamish.serverbot2.sharedconfig;

/**
 * A deferred configuration value that fetches the system environment variable with the given name.
 */
public class EnvVar extends ConfigValue {

    public EnvVar(String name) {
        super(name, System::getenv);
    }

}
