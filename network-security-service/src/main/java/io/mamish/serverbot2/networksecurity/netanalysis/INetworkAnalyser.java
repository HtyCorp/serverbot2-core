package io.mamish.serverbot2.networksecurity.netanalysis;

import io.mamish.serverbot2.networksecurity.model.GetNetworkUsageResponse;
import io.mamish.serverbot2.networksecurity.model.PortPermission;

import java.util.List;

public interface INetworkAnalyser {

    GetNetworkUsageResponse analyse(List<PortPermission> authorisedPorts, String endpointVpcIp, int windowSeconds);

}
