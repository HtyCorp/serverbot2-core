package io.mamish.serverbot2.gamemetadata.metastore;

import java.util.Optional;
import java.util.stream.Stream;

public interface IMetadataStore {

    GameMetadataBean get(String key);
    Stream<GameMetadataBean> getAll();
    Optional<GameMetadataBean> getInstanceIdIndex(String instanceId);
    void putIfMissing(GameMetadataBean item) throws StoreConditionException;
    void update(GameMetadataBean item);
    void updateIfStopped(GameMetadataBean item, boolean isStopped) throws StoreConditionException;
    GameMetadataBean deleteIfStopped(String key, boolean isStopped) throws StoreConditionException;

}
