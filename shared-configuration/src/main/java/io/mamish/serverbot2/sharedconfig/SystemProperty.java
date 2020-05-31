package io.mamish.serverbot2.sharedconfig;

public class SystemProperty extends ConfigValue {

    public SystemProperty(String name) {
        super(name, System::getProperty);
    }

}
