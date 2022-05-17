package io.split.openfeature;

import dev.openfeature.javasdk.exceptions.GeneralError;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class SplitModule {
    private static final Logger _log = LoggerFactory.getLogger(SplitModule.class);

    private static SplitModule instance = null;

    private io.split.client.SplitClient client;

    private SplitModule() {}

    public void init(String apiKey) {
        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactory splitFactory;
        try {
            splitFactory = SplitFactoryBuilder.build(apiKey, config);
        } catch (IOException | URISyntaxException e) {
            // exception occurred
            throw new GeneralError("Error occurred creating split factory", e);
        }
        this.client = splitFactory.client();
        try {
            this.client.blockUntilReady();
        } catch (InterruptedException e) {
            _log.error("Interrupted Exception: ", e);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            throw new GeneralError("Error occurred initializing the client.", e);
        }
    }

    public static SplitModule getInstance() {
        if (instance == null) {
            instance = new SplitModule();
        }
        return instance;
    }

    public io.split.client.SplitClient getClient() {
        return client;
    }
}
