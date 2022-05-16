package io.split.openfeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.FeatureProvider;
import dev.openfeature.javasdk.FlagEvaluationDetails;
import dev.openfeature.javasdk.FlagEvaluationOptions;
import dev.openfeature.javasdk.FlagValueType;
import dev.openfeature.javasdk.HookContext;
import dev.openfeature.javasdk.ProviderEvaluation;

import dev.openfeature.javasdk.Reason;
import dev.openfeature.javasdk.exceptions.GeneralError;
import io.split.client.SplitClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SplitProvider implements FeatureProvider {

    // TODO: implement error code that we return rather than throw generic error

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
        HookContext<Boolean> context = constructContext(key, FlagValueType.BOOLEAN, evaluationContext, defaultTreatment);
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions, context);
            Boolean value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                // if treatment is "on" we treat that as true
                value = Boolean.parseBoolean(evaluated) || evaluated.equals("on");
            }
            Reason reason = Reason.SPLIT;
            String variant = null;
            FlagEvaluationDetails<Boolean> flagEvaluationDetails = FlagEvaluationDetails.<Boolean>builder()
                    .flagKey(key)
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
            runAfterHooks(context, flagEvaluationDetails, flagEvaluationOptions);

            ProviderEvaluation.ProviderEvaluationBuilder<Boolean> builder = ProviderEvaluation.builder();
            // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
            // TODO: is there a way to get the description here to use as variant?
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
        } catch (Exception e) {
            runErrorHooks(context, e, flagEvaluationOptions);
            throw new GeneralError("Error getting boolean evaluation", e);
        } finally {
            runFinallyHooks(context, flagEvaluationOptions);
        }

    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        HookContext<String> context = constructContext(key, FlagValueType.STRING, evaluationContext, defaultTreatment);
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions, context);
            String value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                value = evaluated;
            }

            Reason reason = Reason.SPLIT;
            String variant = null;
            FlagEvaluationDetails<String> flagEvaluationDetails = FlagEvaluationDetails.<String>builder()
                    .flagKey(key)
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
            runAfterHooks(context, flagEvaluationDetails, flagEvaluationOptions);

            ProviderEvaluation.ProviderEvaluationBuilder<String> builder = ProviderEvaluation.builder();
            // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
            // TODO: is there a way to get the description here to use as variant?
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
        } catch (Exception e) {
            runErrorHooks(context, e, flagEvaluationOptions);
            throw new GeneralError("Error getting String evaluation", e);
        } finally {
            runFinallyHooks(context, flagEvaluationOptions);
        }
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        HookContext<Integer> context = constructContext(key, FlagValueType.INTEGER, evaluationContext, defaultTreatment);
        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions, context);
            Integer value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                value = Integer.valueOf(evaluated);
            }

            Reason reason = Reason.SPLIT;
            String variant = null;
            FlagEvaluationDetails<Integer> flagEvaluationDetails = FlagEvaluationDetails.<Integer>builder()
                    .flagKey(key)
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
            runAfterHooks(context, flagEvaluationDetails, flagEvaluationOptions);

            ProviderEvaluation.ProviderEvaluationBuilder<Integer> builder = ProviderEvaluation.builder();
            // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
            // TODO: is there a way to get the description here to use as variant?
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
        } catch (Exception e) {
            runErrorHooks(context, e, flagEvaluationOptions);
            throw new GeneralError("Error getting Integer evaluation", e);
        } finally {
            runFinallyHooks(context, flagEvaluationOptions);
        }
    }

    // Should this be a part of the interface??
    public <T> ProviderEvaluation<T> getEvaluation(String key, T defaultTreatment, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions) {
        HookContext<T> context = constructContext(key, FlagValueType.OBJECT, evaluationContext, defaultTreatment);

        try {
            String evaluated = evaluateTreatment(key, evaluationContext, flagEvaluationOptions, context);
            T value;
            if (noTreatment(evaluated)) {
                value = defaultTreatment;
            } else {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    value = objectMapper.readValue(evaluated, new TypeReference<T>() {
                    });
                } catch (JsonProcessingException e) {
                    throw new GeneralError("Error reading treatment as requested type.", e);
                }
            }

            Reason reason = Reason.SPLIT;
            String variant = null;
            FlagEvaluationDetails<T> flagEvaluationDetails = FlagEvaluationDetails.<T>builder()
                    .flagKey(key)
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
            runAfterHooks(context, flagEvaluationDetails, flagEvaluationOptions);

            ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
            // TODO: Is there a way to get the reason? Something like DEFAULT or SPLIT?
            // TODO: is there a way to get the description here to use as variant?
            return builder
                    .value(value)
                    .reason(reason)
                    .variant(variant)
                    .build();
        } catch (Exception e) {
            runErrorHooks(context, e, flagEvaluationOptions);
            throw new GeneralError("Error getting Object evaluation", e);
        } finally {
            runFinallyHooks(context, flagEvaluationOptions);
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

    private <T> String evaluateTreatment(String key, EvaluationContext evaluationContext, FlagEvaluationOptions flagEvaluationOptions, HookContext<T> hookContext) {
        // first run before hooks
        runBeforeHooks(hookContext, flagEvaluationOptions);
        // TODO: get id from evaluation context once that class is defined, and get attributes to pass into split client
        String id = "someId";
        Map<String, Object> attributes = transformContext(evaluationContext);
        String treatment = client.getTreatment(id, key, attributes);
        // run after hooks
        return treatment;
    }

    private boolean noTreatment(String treatment) {
        return treatment == null || treatment.isEmpty() || treatment.equals("control");
    }

    /* Run Hooks. Not to sure this is supposed to be done in Provider (but I think so). */
    // TODO:

    private <T> HookContext<T> constructContext(String flag, FlagValueType flagType, EvaluationContext evaluationContext, T defaultValue) {
        return HookContext.<T>builder()
                .flagKey(flag)
                .type(flagType)
                .ctx(evaluationContext)
                .defaultValue(defaultValue)
                .build();
    }

    private <T> void runBeforeHooks(HookContext<T> hookContext, FlagEvaluationOptions flagEvaluationOptions) {
        ImmutableMap<String, Object> hookHints = flagEvaluationOptions.getHookHints();
        flagEvaluationOptions.getHooks()
                .forEach(hook -> hook.before(hookContext, hookHints));
    }

    private <T> void runAfterHooks(HookContext<T> hookContext, FlagEvaluationDetails<T> flagEvaluationDetails, FlagEvaluationOptions flagEvaluationOptions) {
        ImmutableMap<String, Object> hookHints = flagEvaluationOptions.getHookHints();
        flagEvaluationOptions.getHooks()
                .forEach(hook -> hook.after(hookContext, flagEvaluationDetails, hookHints));
    }

    private <T> void runErrorHooks(HookContext<T> hookContext, Exception e, FlagEvaluationOptions flagEvaluationOptions) {
        ImmutableMap<String, Object> hookHints = flagEvaluationOptions.getHookHints();
        flagEvaluationOptions.getHooks()
                .forEach(hook -> hook.error(hookContext, e, hookHints));
    }

    private <T> void runFinallyHooks(HookContext<T> hookContext, FlagEvaluationOptions flagEvaluationOptions) {
        ImmutableMap<String, Object> hookHints = flagEvaluationOptions.getHookHints();
        flagEvaluationOptions.getHooks()
                .forEach(hook -> hook.finallyAfter(hookContext, hookHints));
    }
}
