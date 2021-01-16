package com.admiralbot.networksecurity.netanalysis;

import com.admiralbot.networksecurity.model.PortPermission;

import java.util.List;
import java.util.Optional;

public interface INetworkAnalyser {

    Optional<Integer> getLatestActivityAgeSeconds(List<String> authorisedIps, List<PortPermission> authorisedPorts,
                                                  String endpointVpcIp, int windowSeconds);

}
