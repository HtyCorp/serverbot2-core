package com.admiralbot.echoservice.model;

public enum StorageType {
    VOLUMES, // EBS volumes
    GAME_VOLUMES, // EBS volumes specifically for game servers
    SNAPSHOTS, // EBS snapshot
}
