package io.split.openfeature;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableMetadata;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.MutableStructure;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.TrackingEventDetails;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.GeneralError;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import dev.openfeature.sdk.exceptions.ParseError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import io.split.client.SplitClient;
import io.split.client.api.SplitResult;
import io.split.openfeature.utils.Serialization;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SplitProvider implements FeatureProvider {

  private static final String NAME = "Split";

  private final SplitClient client;

  public SplitProvider(SplitClient splitClient) {
    client = splitClient;
  }

  public SplitProvider(String apiKey) {
    SplitModule splitModule = SplitModule.getInstance();
    if (splitModule.getClient() == null) {
      splitModule.init(apiKey);
    }
    client = splitModule.getClient();
  }

  @Override
  public Metadata getMetadata() {
    return () -> NAME;
  }

  @Override
  public ProviderEvaluation<Boolean> getBooleanEvaluation(
          String key, Boolean defaultVal, EvaluationContext ctx) {
    return getEvaluation(key, defaultVal, ctx, s -> {
      // if treatment is "on" or "true" we treat that as true
      // if it is "off" or "false" we treat it as false
      // if it is some other value we throw an error (sdk will catch it and throw default treatment)
      if (Boolean.parseBoolean(s) || s.equals("on")) {
        return true;
      } else if (s.equalsIgnoreCase("false") || s.equals("off")) {
        return false;
      } else {
        throw new ParseError();
      }
    }, "Boolean");
  }

  @Override
  public ProviderEvaluation<String> getStringEvaluation(
          String key, String defaultVal, EvaluationContext ctx) {
    return getEvaluation(key, defaultVal, ctx, s -> s, "String");
  }

  @Override
  public ProviderEvaluation<Integer> getIntegerEvaluation(
          String key, Integer defaultVal, EvaluationContext ctx) {
    return getEvaluation(key, defaultVal, ctx, Integer::valueOf, "Integer");
  }

  @Override
  public ProviderEvaluation<Double> getDoubleEvaluation(
          String key, Double defaultTreatment, EvaluationContext ctx) {

    return getEvaluation(key, defaultTreatment, ctx, s -> {
      if (s == null) throw new NumberFormatException("null");
      return Double.valueOf(s.trim());
    }, "Double");
  }

  @Override
  public ProviderEvaluation<Value> getObjectEvaluation(
          String key, Value defaultVal, EvaluationContext ctx) {
    return getEvaluation(key, defaultVal, ctx, s -> {
      Map<String, Object> rawMap = Serialization.stringToMap(s);
      return mapToValue(rawMap);
    }, "Object");
  }

  @FunctionalInterface
  interface Mapper<T> {
    T map(String s) throws Exception;
  }

  private <T> ProviderEvaluation<T> getEvaluation(
          String key,
          T defaultValue,
          EvaluationContext ctx,
          Mapper<T> mapper,
          String typeLabel
  ) {
    try {
      SplitResult evaluated = evaluateTreatment(key, ctx);
      String treatment = evaluated.treatment();
      String config = evaluated.config();
      ImmutableMetadata metadata = ImmutableMetadata.builder().addString("config", config).build();

      if (noTreatment(treatment)) {
        return constructProviderEvaluation(
                defaultValue, treatment, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND, metadata);
      }
      T mapped = mapper.map(treatment);
      return constructProviderEvaluation(mapped, treatment, metadata);

    } catch (OpenFeatureError e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralError(String.format("Error getting %s evaluation", typeLabel), e);
    }
  }

  @Override
  public void track(String eventName, EvaluationContext context, TrackingEventDetails details) {

    // targetingKey is always required
    String key = context.getTargetingKey();
    if (key == null || key.isEmpty()) throw new TargetingKeyMissingError();

    // eventName is always required
    if (eventName == null || eventName.isBlank()) throw new GeneralError("Missing eventName, required to track");

    // trafficType is always required
    Value ttVal = context.getValue("trafficType");
    String trafficType = (ttVal != null && !ttVal.isNull() && ttVal.isString()) ? ttVal.asString() : null;
    if (trafficType == null || trafficType.isBlank()) throw new GeneralError("Missing trafficType variable, required to track");

    double value = 0;
    Map<String, Object> attributes = new HashMap<>();
    if (details != null) {
      Optional<Number> optionalValue = details.getValue();
      value = optionalValue.orElse(0).doubleValue();
      attributes = details.asObjectMap();
    }

    client.track(key, trafficType, eventName, value, attributes);
  }

  public Map<String, Object> transformContext(EvaluationContext context) {
    return context.asObjectMap();
  }

  @Override
  public void shutdown() {
    client.destroy();
  }

  private SplitResult evaluateTreatment(String key, EvaluationContext evaluationContext) {
    String id = evaluationContext.getTargetingKey();
    if (id == null || id.isEmpty()) {
      // targeting key is always required
      throw new TargetingKeyMissingError();
    }
    Map<String, Object> attributes = transformContext(evaluationContext);
    return client.getTreatmentWithConfig(id, key, attributes);
  }

  private boolean noTreatment(String treatment) {
    return treatment == null || treatment.isEmpty() || treatment.equals("control");
  }

  private <T> ProviderEvaluation<T> constructProviderEvaluation(T value, String variant, ImmutableMetadata metadata) {
    return constructProviderEvaluation(value, variant, Reason.TARGETING_MATCH, null, metadata);
  }

  private <T> ProviderEvaluation<T> constructProviderEvaluation(T value, String variant, Reason reason, ErrorCode errorCode, ImmutableMetadata metadata) {
    ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
    return builder
      .value(value)
      .flagMetadata(metadata)
      .reason(reason.name())
      .variant(variant)
      .errorCode(errorCode)
      .build();
  }

  /**
   * Turn map String->Object into a Value.
   * @param map a Map String->Object, where object is NOT Value or Structure
   * @return Value representing the map passed in
   */
  private Value mapToValue(Map<String, Object> map) {
    return new Value(
      new MutableStructure(
        map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> objectToValue(e.getValue())))));
  }


  private Value objectToValue(Object object) {
    if (object instanceof String) {
      // try to parse as instant, otherwise use as string
      try {
        return new Value(Instant.parse((String) object));
      } catch (DateTimeParseException e) {
        return new Value((String) object);
      }
    } else if (object instanceof List) {
      // need to translate each elem in list to a value
      return new Value(((List<Object>) object).stream().map(this::objectToValue).collect(Collectors.toList()));
    } else if (object instanceof Map) {
      return mapToValue((Map<String, Object>) object);
    } else {
      try {
        return new Value(object);
      } catch (InstantiationException e) {
        throw new ClassCastException("Could not cast Object to Value");
      }
    }
  }
}
