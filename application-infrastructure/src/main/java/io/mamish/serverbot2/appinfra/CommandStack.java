package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.lambda.Function;

public class CommandStack extends Stack {
    public CommandStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public CommandStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);



        Function function = Function.Builder.create(this, "CommandFunction")
                //TODO
                .build();
    }
}
