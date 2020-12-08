package io.mamish.serverbot2.framework.common;

public interface ServiceInterfaceExample extends ServiceInterface {



    default Object someObject() {
        return null;
    }

}
