package io.mamish.serverbot2.appdaemon.model;

public interface IAppDaemon {

    StartAppResponse startApp(StartAppRequest request);
    StopAppResponse stopApp(StopAppRequest request);

}
