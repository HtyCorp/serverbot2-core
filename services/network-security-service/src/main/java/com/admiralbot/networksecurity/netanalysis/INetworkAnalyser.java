package com.admiralbot.networksecurity.netanalysis;

import com.admiralbot.networksecurity.model.GetNetworkUsageResponse;
import com.admiralbot.networksecurity.model.PortPermission;

import java.util.List;

public interface INetworkAnalyser {

    GetNetworkUsageResponse analyse(List<PortPermission> authorisedPorts, String endpointVpcIp, int windowSeconds);

}
