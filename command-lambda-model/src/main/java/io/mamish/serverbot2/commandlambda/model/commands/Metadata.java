package io.mamish.serverbot2.commandlambda.model.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Metadata {

    private Metadata() {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Command {
        int docsPosition();
        String name();
        int numMinArguments();
        String description();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Argument {
        int argPosition();
        String name();
        String description();
    }

}
