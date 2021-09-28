package com.admiralbot.sharedutil;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Predicate;

public class Poller<PollerInputType,PollerOutputType> {

    private final static Logger logger = LoggerFactory.getLogger(Poller.class);

    private final Function<PollerInputType,PollerOutputType> pollFunction;
    private final long pollIntervalMillis;
    private final long maxAttempts;

    public Poller(Function<PollerInputType, PollerOutputType> pollFunction, long pollIntervalMillis, long maxAttempts) {
        this.pollFunction = pollFunction;
        if (pollIntervalMillis <= 0) {
            throw new IllegalArgumentException("Interval millis must be positive and non-zero");
        }
        this.pollIntervalMillis = pollIntervalMillis;
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts count must be positive and non-zero");
        }
        this.maxAttempts = maxAttempts;
    }

    public void pollUntil(PollerInputType input, Predicate<PollerOutputType> stopCondition) {
        pollUntil(input, stopCondition, null);
    }

    public <ResultType> ResultType pollUntil(PollerInputType input, Predicate<PollerOutputType> stopCondition,
                                Function<PollerOutputType,ResultType> optionalMapper) {
        for (long attemptNum = 0; attemptNum < maxAttempts; attemptNum++) {

            // Note input will usually (but not always) be a simple ID string
            logger.info("Polling attempt #{} with input '{}'...", attemptNum, input);
            PollerOutputType output = pollFunction.apply(input);

            if (stopCondition.test(output)) {
                if (optionalMapper != null) {
                    logger.info("Output passed stop condition - mapping to return value");
                    return optionalMapper.apply(output);
                } else {
                    logger.info("Output passed stop condition - no mapper provided so returning null");
                    return null;
                }
            }

            logger.info("Output didn't pass stop condition, sleeping {}ms...", pollIntervalMillis);

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                logger.error("Unexpected thread interrupt while polling", e);
                Thread.currentThread().interrupt();
            }

        }

        logger.error("Poller exceeded maximum attempt count of {}", maxAttempts);
        // This would throw a TimeoutException for clarity, but it's checked and I don't feel like dealing with that.
        throw new RuntimeException("Poller exceeded maximum attempt count");

    }

}
