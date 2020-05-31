package io.mamish.serverbot2.gamemetadata;

import java.util.Optional;
import java.util.stream.Stream;

public interface IMetadataStore {

    GameMetadataBean get(String key);
    Stream<GameMetadataBean> getAll();
    Optional<GameMetadataBean> getInstanceIdIndex(String instanceId);
    void putIfMissing(GameMetadataBean item) throws StoreConditionException;
    void updateIfStopped(GameMetadataBean item, boolean isStopped) throws StoreConditionException;
    GameMetadataBean deleteIfStopped(String key, boolean isStopped) throws StoreConditionException;

}
