package io.mamish.serverbot2.framework;

import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.server.SqsApiServer;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.XrayUtils;
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
