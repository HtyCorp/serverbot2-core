package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.core.App;

public class CoreApp {
    public static void main(String[] args) {
        App app = new App();
        new CommandStack(app, "CommandStack");
        app.synth();
    }
}
