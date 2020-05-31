package io.mamish.serverbot2.gamemetadata;

import io.mamish.serverbot2.gamemetadata.model.GameReadyState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class LocalSessionMetadataStore implements IMetadataStore {

    Map<String,GameMetadataBean> localStore = new HashMap<>();

    @Override
    public GameMetadataBean get(String key) {
        // Return a copy of the stored item (i.e. don't expose it directly).
        GameMetadataBean storedItem = localStore.get(key);
        return (storedItem == null) ? null : new GameMetadataBean(storedItem);
    }

    @Override
    public Stream<GameMetadataBean> getAll() {
        // Stream which returns copies of stored items
        // Deliberately shuffled since DDB doesn't guarantee order (I don't think...)
        return localStore.values().stream().map(GameMetadataBean::new).sorted(((_o1, _o2) ->
                ThreadLocalRandom.current().nextInt()));
    }

    @Override
    public Optional<GameMetadataBean> getInstanceIdIndex(String instanceId) {
        return localStore.values().stream()
                .filter(item -> item.getInstanceId().equals(instanceId))
                .findFirst()
                .map(GameMetadataBean::new);
    }

    @Override
    public void putIfMissing(GameMetadataBean item) {
        if (localStore.containsKey(item.getGameName())) {
            throw new StoreConditionException("Item name '" + item.getGameName() + "' already exists");
        }
        localStore.put(item.getGameName(), new GameMetadataBean(item));
    }

    @Override
    public void updateIfStopped(GameMetadataBean item, boolean isStopped) {
        GameMetadataBean currentOrNull = localStore.get(item.getGameName());
        throwConditionExceptionIfWrongState(currentOrNull, isStopped);
        if (currentOrNull == null) {
            GameMetadataBean newItem = new GameMetadataBean(item);
            localStore.put(newItem.getGameName(), newItem);
        } else {
            GameMetadataBean updatedItem = new GameMetadataBean(currentOrNull);
            updatedItem.updateFromOtherBean(item);
            localStore.put(updatedItem.getGameName(), updatedItem);
        }
    }

    @Override
    public GameMetadataBean deleteIfStopped(String key, boolean isStopped) {
        GameMetadataBean currentOrNull = localStore.get(key);
        throwConditionExceptionIfWrongState(currentOrNull, isStopped);
        return new GameMetadataBean(currentOrNull);
    }

    private void throwConditionExceptionIfWrongState(GameMetadataBean item, boolean expectingStopped) {
        // Note this deliberately does not complain if the item is null.
        // Might cause minor test result differences when simulating delete/update on missing items.
        if (item != null) {
            boolean stateIsStopped = item.getGameReadyState() == GameReadyState.STOPPED;
            if (stateIsStopped != expectingStopped) {
                throw new StoreConditionException("Item stop state doesn't match requirement");
            }
        }
    }
}
