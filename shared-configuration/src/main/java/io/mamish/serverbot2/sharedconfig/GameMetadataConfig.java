package io.mamish.serverbot2.sharedconfig;

public class GameMetadataConfig {

    public static final String TABLE_NAME = "GameMetadataTable";
    public static final String PARTITION_KEY_MAIN = "gameName";
    public static final String PARTITION_KEY_INSTANCE_ID_INDEX = "instanceId";
    public static final String NAME_INSTANCE_ID_INDEX = "allByInstanceId";

}
