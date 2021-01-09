package com.admiralbot.urlshortener.shortener;

import com.admiralbot.sharedutil.Pair;

public interface ITokenProcessor<InfoBeanType> {

    Pair<String, InfoBeanType> generateTokenAndBean(String url, long ttlSeconds);
    String extractIdFromToken(String token);
    String extractFullUrlFromTokenAndBean(String token, InfoBeanType infoBean);

}
