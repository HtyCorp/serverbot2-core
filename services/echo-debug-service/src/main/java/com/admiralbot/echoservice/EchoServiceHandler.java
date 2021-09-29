package com.admiralbot.echoservice;

import com.admiralbot.echoservice.model.*;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.gamemetadata.model.GameMetadata;
import com.admiralbot.gamemetadata.model.IGameMetadataService;
import com.admiralbot.gamemetadata.model.ListGamesRequest;
import com.admiralbot.sharedutil.SdkUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.util.List;
import java.util.stream.Collectors;

public class EchoServiceHandler implements IEchoService {

    private final Ec2Client ec2Client = SdkUtils.client(Ec2Client.builder());
    private final IGameMetadataService gameMetadataServiceClient = ApiClient.http(IGameMetadataService.class);

    @Override
    public EchoResponse echo(EchoRequest request) {
        String echoMessage = request.getMessage();
        if (request.getShout()) {
            echoMessage = echoMessage.toUpperCase();
        }
        return new EchoResponse(echoMessage);
    }

    @Override
    public HowMuchStorageResponse howMuchStorage(HowMuchStorageRequest request) {
        List<Integer> objectSizes = getObjectSizes(request.getStorageType());
        return new HowMuchStorageResponse(
                request.getStorageType(),
                objectSizes.size(),
                objectSizes.stream().mapToInt(Integer::intValue).sum()
        );
    }

    private List<Integer> getObjectSizes(StorageType storageType) {
        switch (storageType) {
            case VOLUMES: return getVolumeSizes();
            case GAME_VOLUMES: return getGameVolumeSizes();
            case SNAPSHOTS: return getSnapshotSizes();
            default: throw new RequestValidationException("Unexpected or null storage type: " + storageType);
        }
    }

    private List<Integer> getVolumeSizes() {
        return ec2Client.describeVolumesPaginator().volumes().stream()
                .map(Volume::size)
                .collect(Collectors.toList());
    }

    // There are faster ways to do this, but using GMS tests that API clients work in native-image
    private List<Integer> getGameVolumeSizes() {
        List<GameMetadata> games = gameMetadataServiceClient.listGames(new ListGamesRequest()).getGames();
        List<String> gameInstanceIds = games.stream()
                .map(GameMetadata::getInstanceId)
                .collect(Collectors.toList());
        Filter instanceIdFilter = Filter.builder().name("attachment.instance-id").values(gameInstanceIds).build();
        return ec2Client.describeVolumesPaginator(r -> r.filters(instanceIdFilter)).volumes().stream()
                .map(Volume::size)
                .collect(Collectors.toList());
    }

    private List<Integer> getSnapshotSizes() {
        return ec2Client.describeSnapshotsPaginator(r -> r.ownerIds("self")).snapshots().stream()
                .map(Snapshot::volumeSize)
                .collect(Collectors.toList());
    }
}
