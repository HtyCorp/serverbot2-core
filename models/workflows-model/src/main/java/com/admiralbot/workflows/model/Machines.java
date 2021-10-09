package com.admiralbot.workflows.model;

import com.admiralbot.nativeimagesupport.annotation.RegisterGsonType;

// Unusual case convention for an Enum: names are used in account resources so should match the standard there.
@RegisterGsonType
public enum Machines {
    CreateGame,
    RunGame,
    EditGame,
    DeleteGame
}
