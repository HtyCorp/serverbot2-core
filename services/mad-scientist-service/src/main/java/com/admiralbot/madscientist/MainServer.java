package com.admiralbot.madscientist;

import com.admiralbot.madscientist.model.IMadScientist;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.XrayUtils;

public class MainServer extends LambdaProxyServer<IMadScientist> {

    public static void main(String[] args) {
        XrayUtils.setIgnoreMissingContext();
        XrayUtils.setServiceName("MadScientist");
        AppContext.setLambda();
        new MainServer();
    }

    @Override
    protected Class<IMadScientist> getModelClass() {
        return IMadScientist.class;
    }

    @Override
    protected IMadScientist createHandlerInstance() {
        return new MadScientistServiceHandler();
    }

}
