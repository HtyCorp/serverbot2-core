package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.StatesConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.stepfunctions.State;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.amazon.awscdk.services.stepfunctions.Succeed;

public class StateMachinesStack extends Stack {

    public StateMachinesStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public StateMachinesStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        makeStartMachine();

    }

    private void makeStartMachine() {

        State state = Succeed.Builder.create(this, "DoNothing")
                .comment("A placeholder task")
                .build();

        StateMachine machine = StateMachine.Builder.create(this, "StartMachine")
                .stateMachineName(StatesConfig.MACHINE_NAME_START_APP)
                .definition(state)
                .build();

    }

}
