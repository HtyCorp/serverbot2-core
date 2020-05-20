package io.mamish.serverbot2.dynamomapper;

public class EqualsCondition {

    private final String attributeName;
    private final Object expectedValue;

    public EqualsCondition(String attributeName, Object expectedValue) {
        this.attributeName = attributeName;
        this.expectedValue = expectedValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }
}
