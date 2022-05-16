package io.split.openfeature;

import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.FeatureProvider;
import dev.openfeature.javasdk.FlagEvaluationOptions;
import dev.openfeature.javasdk.ProviderEvaluation;

import dev.openfeature.javasdk.Reason;
import io.split.client.SplitFactoryBuilder;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public class SplitProvider implements FeatureProvider {

    private static final Logger _log = LoggerFactory.getLogger(SplitProvider.class);

    private String name;
    private SplitClient client;

    // Interesting that we have to define a constructor... would it make more sense for feature provider to be an abstract class with constructor instead of interface?
    public SplitProvider(String apiKey) {
        this.name = "split";

        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .build();
        SplitFactory splitFactory;
        try {
            splitFactory = SplitFactoryBuilder.build(apiKey, config);
        } catch (IOException | URISyntaxException e) {
            // exception occurred
            _log.error("Error occurred creating split factory", e);
            throw new RuntimeException("Error occurred creating split factory", e);
        }
        this.client = splitFactory.client();
        try {
            this.client.blockUntilReady();
        } catch (TimeoutException | InterruptedException e) {
            // log & handle
            _log.error("Error occurred initializing the client.", e);
            throw new RuntimeException("Error occurred creating split factory", e);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        String evaluated = evaluateTreatment(key, evaluationContext);
        Boolean value;
        if (evaluated == null || evaluated.isEmpty()) {
            value = defaultTreatment;
        } else {
            value = Boolean.valueOf(evaluated);
        }

        ProviderEvaluation.ProviderEvaluationBuilder<Boolean> builder = ProviderEvaluation.builder();
        // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
        // TODO: is there a way to get the description here to use as variant?
        return builder
                .value(value)
                .reason(Reason.SPLIT)
                .build();

    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        String evaluated = evaluateTreatment(key, evaluationContext);
        String value;
        if (evaluated == null || evaluated.isEmpty()) {
            value = defaultTreatment;
        } else {
            value = evaluated;
        }

        ProviderEvaluation.ProviderEvaluationBuilder<String> builder = ProviderEvaluation.builder();
        // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
        // TODO: is there a way to get the description here to use as variant?
        return builder
                .value(value)
                .reason(Reason.SPLIT)
                .build();
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        String evaluated = evaluateTreatment(key, evaluationContext);
        Integer value;
        if (evaluated == null || evaluated.isEmpty()) {
            value = defaultTreatment;
        } else {
            value = Integer.valueOf(evaluated);
        }

        ProviderEvaluation.ProviderEvaluationBuilder<Integer> builder = ProviderEvaluation.builder();
        // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
        // TODO: is there a way to get the description here to use as variant?
        return builder
                .value(value)
                .reason(Reason.SPLIT)
                .build();
    }

    // Should this be a part of the interface??
    public <T> ProviderEvaluation<T> getEvaluation(String key, T defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        String evaluated = evaluateTreatment(key, evaluationContext);
        T value;
        if (evaluated == null || evaluated.isEmpty()) {
            value = defaultTreatment;
        } else {
            // FIXME - is this the best thing to do
            value = (T) evaluated;
        }

        ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
        // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
        // TODO: is there a way to get the description here to use as variant?
        return builder
                .value(value)
                .reason(Reason.SPLIT)
                .build();
    }

    private String evaluateTreatment(String key, EvaluationContext evaluationContext) {
        // TODO: get id from evaluation context once that class is defined
        String id = "someId";
        // TODO: do we need the third arg map?
        return client.getTreatment(id, key);
    }
}
