package io.mamish.serverbot2.appdaemon.model;

import io.mamish.serverbot2.framework.common.ApiRequestInfo;

@ApiRequestInfo(order = 1, name = "StopApp", numRequiredFields = 0, description = "Stop the app configured on this server")
public class StopAppRequest {
}
