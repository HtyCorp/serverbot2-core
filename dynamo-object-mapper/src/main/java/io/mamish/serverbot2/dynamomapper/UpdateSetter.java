package io.mamish.serverbot2.dynamomapper;

public class UpdateSetter {

    private final String attributeName;
    private final Object newValue;

    public UpdateSetter(String attributeName, Object newValue) {
        this.attributeName = attributeName;
        this.newValue = newValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Object getNewValue() {
        return newValue;
    }
}
