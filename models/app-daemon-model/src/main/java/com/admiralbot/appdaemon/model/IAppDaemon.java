package com.admiralbot.appdaemon.model;

// This interface is pretty unique and doesn't work like a normal service, so no endpoint info is defined.
// Will eventually be replaced with a 'gateway' service to standardise things.
public interface IAppDaemon {

    StartAppResponse startApp(StartAppRequest request);
    StopAppResponse stopApp(StopAppRequest request);
    StartSftpServerResponse startSftpServer(StartSftpServerRequest request);
    ExtendDiskResponse extendDisk(ExtendDiskRequest request);


}
