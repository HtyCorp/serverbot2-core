package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.App;

public class CoreApp {

    public static void main(String[] args) {

        App app = new App();

        CommonStack commonStack = new CommonStack(app, "CommonStack");
        new IpStack(app, "IpStack", commonStack);
        new RelayStack(app, "RelayStack");
        new CommandStack(app, "CommandStack");

        app.synth();

    }

}
