package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class DaemonStack extends Stack {

    public DaemonStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public DaemonStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);



    }

}
