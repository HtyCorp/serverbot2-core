package io.mamish.serverbot2.urlshortener;

import io.mamish.serverbot2.sharedutil.Pair;

public interface IUrlProcessor<InfoBeanType> {

    Pair<String, InfoBeanType> generateTokenAndBean(String url, long ttlSeconds);
    String extractIdFromToken(String token);
    String extractFullUrlFromTokenAndBean(String token, InfoBeanType infoBean);

}
