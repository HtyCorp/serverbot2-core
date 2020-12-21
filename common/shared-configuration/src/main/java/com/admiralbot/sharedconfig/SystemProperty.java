package com.admiralbot.sharedconfig;

public class SystemProperty extends ConfigValue {

    public SystemProperty(String name) {
        super(name, System::getProperty);
    }

}
