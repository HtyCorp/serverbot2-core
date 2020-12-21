package com.admiralbot.framework;

import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.server.SqsApiServer;
import com.admiralbot.sharedutil.IDUtils;
import com.admiralbot.sharedutil.XrayUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsTests {

    void disabledTestSendReceive() {
        XrayUtils.setInstrumentationEnabled(false);

        IDummyService implementation = request -> {
            System.out.println("Got a request");
            return new DummyResponse();
        };

        SqsClient sqs = SqsClient.create();
        String queueName = "TempTestQueue-" + IDUtils.randomUUIDJoined();
        String queueUrl = sqs.createQueue(r -> r.queueName(queueName)).queueUrl();

        SqsApiServer<IDummyService> service = new SqsApiServer<>(queueName) {
            @Override
            protected Class<IDummyService> getModelClass() {
                return IDummyService.class;
            }

            @Override
            protected IDummyService createHandlerInstance() {
                return implementation;
            }
        };

        IDummyService client = ApiClient.sqs(IDummyService.class, queueName);

        client.dummy(new DummyRequest());

        sqs.deleteQueue(r -> r.queueUrl(queueUrl));

        System.out.println("Done!");

    }

}
