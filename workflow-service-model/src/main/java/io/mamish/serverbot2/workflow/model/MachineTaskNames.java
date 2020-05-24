package io.mamish.serverbot2.workflow.model;

// Unusual case convention for an Enum: names are used in account resources so should match the standard there.
public enum MachineTaskNames {
    CreateGameMetadata,
    LockGame,
    CreateGameResources,
    StartInstance,
    WaitInstanceReady,
    StartServer,
    WaitServerStop,
    StopInstance,
    DeleteGameResources
}
