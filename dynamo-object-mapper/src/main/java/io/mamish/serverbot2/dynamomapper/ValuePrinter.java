package io.mamish.serverbot2.dynamomapper;

class ValuePrinter {

    String printObject(Object inputObject) {
        if (inputObject instanceof Enum) {
            return inputObject.toString();
        } else if (inputObject instanceof String) {
            return (String) inputObject;
        } else {
            throw new IllegalArgumentException("Input is not a supported class for printing: "
                    + inputObject.getClass().getCanonicalName());
        }
    }

}
