package io.mamish.serverbot2.workflows.model;

// Unusual case convention for an Enum: names are used in account resources so should match the standard there.
public enum Tasks {
    CreateGameMetadata,
    LockGame,
    CreateGameResources,
    StartInstance,
    WaitInstanceReady,
    InstanceReadyNotify,
    InstanceReadyStartServer,
    WaitServerStop,
    StopInstance,
    DeleteGameResources
}
