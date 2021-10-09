package com.admiralbot.workflows.model;

import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

// Unusual case convention for an Enum: names are used in account resources so should match the standard there.
@RegisterGsonType
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
