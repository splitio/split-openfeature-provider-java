package io.split.openfeature;

import dev.openfeature.javasdk.ErrorCode;
import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.FeatureProvider;
import dev.openfeature.javasdk.Metadata;
import dev.openfeature.javasdk.ProviderEvaluation;
import dev.openfeature.javasdk.Reason;
import dev.openfeature.javasdk.Structure;
import dev.openfeature.javasdk.Value;
import dev.openfeature.javasdk.exceptions.GeneralError;
import dev.openfeature.javasdk.exceptions.OpenFeatureError;
import dev.openfeature.javasdk.exceptions.ParseError;
import io.split.client.SplitClient;
import io.split.openfeature.utils.Serialization;

import java.util.HashMap;
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
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND.name());
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
        throw new ParseError(ErrorCode.PARSE_ERROR.name());
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
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND.name());
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
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND.name());
      }
      Integer value = Integer.valueOf(evaluated);
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new ParseError(ErrorCode.PARSE_ERROR.name());
    } catch (Exception e) {
      throw new GeneralError("Error getting Integer evaluation", e);
    }
  }

  @Override
  public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND.name());
      }
      Double value = Double.valueOf(evaluated);
      return constructProviderEvaluation(value, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new ParseError(ErrorCode.PARSE_ERROR.name());
    } catch (Exception e) {
      throw new GeneralError("Error getting Double evaluation", e);
    }
  }

  @Override
  public ProviderEvaluation<Structure> getObjectEvaluation(String key, Structure defaultTreatment, EvaluationContext evaluationContext) {
    try {
      String evaluated = evaluateTreatment(key, evaluationContext);
      if (noTreatment(evaluated)) {
        return constructProviderEvaluation(defaultTreatment, evaluated, Reason.DEFAULT, ErrorCode.FLAG_NOT_FOUND.name());
      }
      Structure structure = new Structure();
      Map<String, Object> rawMap = Serialization.stringToMap(evaluated);
      for (Map.Entry<String, Object> rawEntry : rawMap.entrySet()) {
        structure.add(rawEntry.getKey(),(String) rawEntry.getValue());
      }
      return constructProviderEvaluation(structure, evaluated);
    } catch (OpenFeatureError e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralError("Error getting Object evaluation", e);
    }
  }

  public Map<String, Object> transformContext(EvaluationContext context) {
    return getMapFromStructMap(context.asMap());
  }

  private String evaluateTreatment(String key, EvaluationContext evaluationContext) {
    String id = evaluationContext.getTargetingKey();
    if (id == null || id.isEmpty()) {
      // targeting key is always required
      throw new GeneralError("TARGETING_KEY_MISSING");
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

  private <T> ProviderEvaluation<T> constructProviderEvaluation(T value, String variant, Reason reason, String errorCode) {
    ProviderEvaluation.ProviderEvaluationBuilder<T> builder = ProviderEvaluation.builder();
    return builder
      .value(value)
      .reason(reason)
      .variant(variant)
      .errorCode(errorCode)
      .build();
  }

  private Map<String, Object> getMapFromStructMap(Map<String, Value> structMap) {
    Map<String, Object> toReturn = new HashMap<>();
    for (Map.Entry<String, Value> entry : structMap.entrySet()) {
      String key = entry.getKey();
      Value value = entry.getValue();
      toReturn.put(key, getInnerValue(value));
    }
    return toReturn;
  }

  private Map<String, Object> getMapFromStructure(Structure structure) {
    return getMapFromStructMap(structure.asMap());
  }

  private Object getInnerValue(Value value) {
    Object object = value.asBoolean();
    if (object != null) {
      return object;
    }
    object = value.asDouble();
    if (object != null) {
      return object;
    }
    object = value.asInteger();
    if (object != null) {
      return object;
    }
    object = value.asString();
    if (object != null) {
      return object;
    }
    object = value.asZonedDateTime();
    if (object != null) {
      return object.toString();
    }
    object = value.asStructure();
    if (object != null) {
      // must return a map
      return getMapFromStructure((Structure) object);
    }
    object = value.asList();
    if (object != null) {
      // must return a list of inner objects
      List<Value> values = (List<Value>) object;
      return values.stream().map(this::getInnerValue).collect(Collectors.toList());
    }
    return null;
  }
}
