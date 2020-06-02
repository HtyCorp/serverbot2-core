package io.mamish.serverbot2.framework;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import io.mamish.serverbot2.framework.client.ApiClient;
import io.mamish.serverbot2.framework.server.SqsApiServer;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsTests {

    void disabledTestSendReceive() {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());

        IDummyService implementation = request -> {
            System.out.println("Got a request");
            return new DummyResponse();
        };

        SqsClient sqs = SqsClient.create();
        String queueName = "TempTestQueue-" + IDUtils.randomUUIDJoined();
        sqs.createQueue(r -> r.queueName(queueName));

        SqsApiServer<IDummyService> service = new SqsApiServer<IDummyService>(queueName) {
            @Override
            protected Class<IDummyService> getModelClass() {
                return IDummyService.class;
            }

            @Override
            protected IDummyService getHandlerInstance() {
                return implementation;
            }
        };

        IDummyService client = ApiClient.sqs(IDummyService.class, queueName);

        client.dummy(new DummyRequest());

        System.out.println("Done!");

    }

}
