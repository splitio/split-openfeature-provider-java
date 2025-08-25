package io.split.openfeature;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.MutableStructure;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.GeneralError;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import dev.openfeature.sdk.exceptions.ParseError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import io.split.client.SplitClient;
import io.split.openfeature.utils.Serialization;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
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
  public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND);
      }
      // if treatment is "on" or "true" we treat that as true
      // if it is "off" or "false" we treat it as false
      // if it is some other value we throw an error (sdk will catch it and throw default treatment)
      boolean value;
      if (Boolean.parseBoolean(evaluated) || evaluated.equals("on")) {
        value = true;
      } else if (evaluated.equalsIgnoreCase("false") || evaluated.equals("off")) {
        value = false;
      } else {
        throw new ParseError();
      }
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralError("Error getting boolean evaluation", e);
    }

  }

  @Override
  public ProviderEvaluation<String> getStringEvaluation(String key, String defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND);
      }
      return constructProviderEvaluation(evaluated, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralError("Error getting String evaluation", e);
    }
  }

  @Override
  public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND);
      }
      Integer value = Integer.valueOf(evaluated);
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new ParseError();
    } catch (Exception e) {
      throw new GeneralError("Error getting Integer evaluation", e);
    }
  }

  @Override
  public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND);
      }
      Double value = Double.valueOf(evaluated);
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new ParseError();
    } catch (Exception e) {
      throw new GeneralError("Error getting Double evaluation", e);
    }
  }

  @Override
  public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND);
      }
      Map<String, Object> rawMap = Serialization.stringToMap(evaluated);
      Value value = mapToValue(rawMap);
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralError("Error getting Object evaluation", e);
    }
  }

  public Map<String, Object> transformContext(EvaluationContext context) {
    return context.asObjectMap();
  }

  private String evaluateTreatment(String key, EvaluationContext evaluationContext) {
    String id = evaluationContext.getTargetingKey();
    if (id == null || id.isEmpty()) {
      // targeting key is always required
      throw new TargetingKeyMissingError();
    }
    Map<String, Object> attributes = transformContext(evaluationContext);
    return client.getTreatment(id, key, attributes);
  }

  private boolean noTreatment(String treatment) {
    return treatment == null || treatment.isEmpty() || treatment.equals("control");
  }

  private <T> ProviderEvaluation<T> constructProviderEvaluation(T value, String variant) {
    return constructProviderEvaluation(value, variant, Reason.TARGETING_MATCH, null);
  }

  private <T> ProviderEvaluation<T> constructProviderEvaluation(T value, String variant, Reason reason, ErrorCode errorCode) {
    ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
    return builder
      .value(value)
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
