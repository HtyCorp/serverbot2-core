package io.mamish.serverbot2.commandlambda;

import io.mamish.serverbot2.commandlambda.model.commands.Metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratedCommandDefinition {

    private String name;

    private Method targetMethod;
    private Constructor<?> requestTypeConstructor;
    private List<Field> orderedFields;
    private int numRequiredFields;

    private int documentationPosition;
    private String usageString;
    private String descriptionString;
    private List<String> argumentDescriptionStrings;


    public GeneratedCommandDefinition(Method targetCommandMethod) throws ReflectiveOperationException {

        Class<?> requestType = targetCommandMethod.getParameterTypes()[0];
        Metadata.Command commandMetadata = requestType.getAnnotation(Metadata.Command.class);
        List<Field> requestTypeFields = Arrays.asList(requestType.getDeclaredFields());
        requestTypeFields.forEach(f -> f.setAccessible(true));
        requestTypeFields.sort(Comparator.comparing(f -> f.getAnnotation(Metadata.Argument.class).argPosition()));

        this.name = commandMetadata.name();
        this.targetMethod = targetCommandMethod;
        this.requestTypeConstructor = requestType.getConstructor();
        this.orderedFields = requestTypeFields;
        this.numRequiredFields = commandMetadata.numMinArguments();

        List<Metadata.Argument> argMetaList = requestTypeFields.stream().map(f -> f.getAnnotation(Metadata.Argument.class)).collect(Collectors.toList());

        StringBuilder sbUsage = new StringBuilder();
        sbUsage.append("!").append(this.name);
        for (int i = 0; i < argMetaList.size(); i++) {
            String argName = argMetaList.get(i).name();
            sbUsage.append(' ');
            if (i < this.numRequiredFields) {
                sbUsage.append(argName);
            } else {
                sbUsage.append('[').append(argName).append(']');
            }
        }

        this.documentationPosition = commandMetadata.docsPosition();
        this.usageString = sbUsage.toString();
        this.descriptionString = commandMetadata.description();
        this.argumentDescriptionStrings = argMetaList.stream().map(m -> m.name() + ": " + m.description()).collect(Collectors.toList());

    }

    public String getName() {
        return name;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public Constructor<?> getRequestTypeConstructor() {
        return requestTypeConstructor;
    }

    public List<Field> getOrderedFields() {
        return orderedFields;
    }

    public int getNumRequiredFields() {
        return numRequiredFields;
    }

    public int getDocumentationPosition() {
        return documentationPosition;
    }

    public String getUsageString() {
        return usageString;
    }

    public String getDescriptionString() {
        return descriptionString;
    }

    public List<String> getArgumentDescriptionStrings() {
        return argumentDescriptionStrings;
    }
}
