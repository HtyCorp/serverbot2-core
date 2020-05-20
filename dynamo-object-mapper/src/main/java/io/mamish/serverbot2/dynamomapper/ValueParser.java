package io.mamish.serverbot2.dynamomapper;

class ValueParser {

    Object parseString(String inputString, Class<?> outputClass) {
        if (Enum.class.isAssignableFrom(outputClass)) {
            return parseEnumType(outputClass, inputString);
        } else if (outputClass.equals(String.class)) {
            return inputString;
        } else {
            throw new IllegalArgumentException("Input is not a supported class for parsing: " + outputClass.getCanonicalName());
        }
    }

    // Parsing Enum classes in a way that generics will agree with is hard. Not worth the effort right now.
    private Object parseEnumType(@SuppressWarnings("rawtypes") Class enumClass, String member) {
        @SuppressWarnings("unchecked")
        Object enumValue = Enum.valueOf(enumClass, member);
        return enumValue;
    }

}
