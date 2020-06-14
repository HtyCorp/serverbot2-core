package io.mamish.serverbot2.networksecurity.netanalysis;

import io.mamish.serverbot2.networksecurity.model.GetNetworkUsageResponse;

import java.util.List;

public interface INetworkAnalyser {

    GetNetworkUsageResponse analyse(List<String> authorisedIps, String endpointVpcIp, int windowSeconds);

}
