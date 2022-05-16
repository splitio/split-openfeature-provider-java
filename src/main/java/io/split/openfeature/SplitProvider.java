package io.split.openfeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.FeatureProvider;
import dev.openfeature.javasdk.FlagEvaluationOptions;
import dev.openfeature.javasdk.ProviderEvaluation;

import dev.openfeature.javasdk.Reason;
import dev.openfeature.javasdk.exceptions.GeneralError;
import io.split.client.SplitClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SplitProvider implements FeatureProvider {

    private static final Logger _log = LoggerFactory.getLogger(SplitProvider.class);

    private final String name;
    private final SplitClient client;

    // Interesting that we have to define a constructor... would it make more sense for feature provider to be an abstract class with constructor instead of interface?
    public SplitProvider(String apiKey) {
        this.name = "split";
        SplitModule splitModule = SplitModule.getInstance();
        if (splitModule.getClient() == null) {
            splitModule.init(apiKey);
        }
        client = splitModule.getClient();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        String evaluated = evaluateTreatment(key, evaluationContext);
        Boolean value;
        if (noTreatment(evaluated)) {
            value = defaultTreatment;
        } else {
            // if treatment is "on" we treat that as true
            value = Boolean.parseBoolean(evaluated) || evaluated.equals("on");
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
        if (noTreatment(evaluated)) {
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
        if (noTreatment(evaluated)) {
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
        if (noTreatment(evaluated)) {
            value = defaultTreatment;
        } else {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                value = objectMapper.readValue(evaluated, new TypeReference<T>() {});
            } catch (JsonProcessingException e) {
                throw new GeneralError("Error reading treatment as requested type.", e);
            }
        }

        ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
        // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
        // TODO: is there a way to get the description here to use as variant?
        return builder
                .value(value)
                .reason(Reason.SPLIT)
                .build();
    }

    public Map<String, Object> transformContext(EvaluationContext context) {
        /* according to spec https://github.com/open-feature/spec/blob/main/specification/provider/providers.md#context-transformation
            we should have this (public) method which translates the context into the format split expects, which is a map of attributes.
            It is public so the client can transform context once and give it to us on calls to save us from doing it each time. (We need to modify some method signatures to allow this then?)
         */
        // TODO: fill in once the EvaluationContext is defined
        return Map.of();
    }

    private String evaluateTreatment(String key, EvaluationContext evaluationContext) {
        // TODO: get id from evaluation context once that class is defined, and get attributes to pass into split client
        String id = "someId";
        Map<String, Object> attributes = transformContext(evaluationContext);
        return client.getTreatment(id, key, attributes);
    }

    private boolean noTreatment(String treatment) {
        return treatment == null || treatment.isEmpty() || treatment.equals("control");
    }
}
