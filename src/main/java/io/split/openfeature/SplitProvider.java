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
import io.split.openfeature.utils.Serialization;

import java.util.Map;

public class SplitProvider implements FeatureProvider {

    private static final String NAME = "split";

    private final SplitClient client;

    public SplitProvider(SplitClient splitClient) {
        client = splitClient;
    }

    // Interesting that we have to define a constructor... would it make more sense for feature provider to be an abstract class with constructor instead of interface?
    public SplitProvider(String apiKey) {
        SplitModule splitModule = SplitModule.getInstance();
        if (splitModule.getClient() == null) {
            splitModule.init(apiKey);
        }
        client = splitModule.getClient();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions);
            Boolean value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                // if treatment is "on" we treat that as true or if it is true
                // if it is false of off we treat it as false
                // if it is some other value we throw an error (sdk will catch it and throw default treatment)
                if (Boolean.parseBoolean(evaluated) || evaluated.equals("on")) {
                    value = true;
                } else if (evaluated.equalsIgnoreCase("false") || evaluated.equals("off")) {
                    value = false;
                } else {
                    throw new GeneralError("Error: Can not cast treatment to a boolean");
                }
            }
            Reason reason = Reason.SPLIT;
            ProviderEvaluation.ProviderEvaluationBuilder<Boolean> builder = ProviderEvaluation.builder();
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(evaluated)
                    .build();
        } catch (Exception e) {
            throw new GeneralError("Error getting boolean evaluation", e);
        }

    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions);
            String value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                value = evaluated;
            }

            Reason reason = Reason.SPLIT;
            ProviderEvaluation.ProviderEvaluationBuilder<String> builder = ProviderEvaluation.builder();
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(evaluated)
                    .build();
        } catch (Exception e) {
            throw new GeneralError("Error getting String evaluation", e);
        }
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions);
            Integer value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                value = Integer.valueOf(evaluated);
            }

            Reason reason = Reason.SPLIT;
            ProviderEvaluation.ProviderEvaluationBuilder<Integer> builder = ProviderEvaluation.builder();
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(evaluated)
                    .build();
        } catch (Exception e) {
            throw new GeneralError("Error getting Integer evaluation", e);
        }
    }

    // Should this be a part of the interface??
    public <T> ProviderEvaluation<T> getStructureValue(String key, T defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions);
            T value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                value = Serialization.deserialize(evaluated, new TypeReference<T>() {});
            }

            Reason reason = Reason.SPLIT;
            ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(evaluated)
                    .build();
        } catch (Exception e) {
            throw new GeneralError("Error getting Object evaluation", e);
        }
    }

    public Map<String, Object> transformContext(EvaluationContext context) {
        /* according to spec https://github.com/open-feature/spec/blob/main/specification/provider/providers.md#context-transformation
            we should have this (public) method which translates the context into the format split expects, which is a map of attributes.
            It is public so the client can transform context once and give it to us on calls to save us from doing it each time. (We need to modify some method signatures to allow this then?)
         */
        // TODO: fill in once the EvaluationContext is defined
        return Map.of();
    }

    private String evaluateTreatment(String key, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        // TODO: get id from evaluation context once that class is defined
        String id = "someId";
        Map<String, Object> attributes = transformContext(evaluationContext);
        return client.getTreatment(id, key, attributes);
    }

    private boolean noTreatment(String treatment) {
        return treatment == null || treatment.isEmpty() || treatment.equals("control");
    }
}
