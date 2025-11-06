package io.split.openfeature;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.MutableStructure;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Value;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class uses the split.yaml file in src/test/resources for the flags and their treatments
 */
public class ClientTest {
  OpenFeatureAPI openFeatureAPI;
  Client client;
  SplitClient splitClient;

  @BeforeEach
  public void init() {
    openFeatureAPI = OpenFeatureAPI.getInstance();
    try {
      SplitClientConfig config = SplitClientConfig
              .builder()
              .splitFile("src/test/resources/split.yaml")
              .setBlockUntilReadyTimeout(10000)
              .build();
      splitClient = SplitFactoryBuilder.build("localhost", config).client();
      openFeatureAPI.setProviderAndWait(new SplitProvider(splitClient));
    } catch (URISyntaxException | IOException e) {
      System.out.println("Unexpected Exception occurred initializing Split Provider.");
    }
    client = openFeatureAPI.getClient("Split Client");
    String targetingKey = "key";
    EvaluationContext evaluationContext = new MutableContext(targetingKey);
    client.setEvaluationContext(evaluationContext);
  }

  @Test
  public void useDefaultTest() {
    // flags that do not exist should return the default value
    String flagName = "random-non-existent-feature";

    Boolean result = client.getBooleanValue(flagName, false);
    assertFalse(result);
    result = client.getBooleanValue(flagName, true);
    assertTrue(result);

    String defaultString = "blah";
    String resultString = client.getStringValue(flagName, defaultString);
    assertEquals(defaultString, resultString);

    int defaultInt = 100;
    Integer resultInt = client.getIntegerValue(flagName, defaultInt);
    assertEquals(defaultInt, resultInt);

    Value defaultStructure = mapToValue(Map.of("foo", new Value("bar")));
    Value resultStructure = client.getObjectValue(flagName, defaultStructure);
    assertEquals(defaultStructure, resultStructure);
  }

  @Test
  public void missingTargetingKeyTest() {
    // Split requires a targeting key and should return the default treatment and throw an error if not provided
    client.setEvaluationContext(new MutableContext());
    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("non-existent-feature", false);
    assertFalse(details.getValue());
    assertEquals(ErrorCode.TARGETING_KEY_MISSING, details.getErrorCode());
  }

  @Test
  public void getControlVariantNonExistentSplit() {
    // split returns a treatment = "control" if the flag is not found.
    // This should be interpreted by the Split provider to mean not found and therefore use the default value.
    // This control treatment should still be recorded as the variant.
    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("non-existent-feature", false);
    assertFalse(details.getValue());
    assertEquals("control", details.getVariant());
    assertEquals(ErrorCode.FLAG_NOT_FOUND, details.getErrorCode());
  }

  @Test
  public void getBooleanSplitTest() {
    // This should be false as defined as "off" in the split.yaml
    Boolean result = client.getBooleanValue("some_other_feature", true);
    assertFalse(result);
  }

  @Test
  public void getBooleanSplitWithKeyTest() {
    // the key "key" was set in the before each. Therefore, the treatment of true should be received as defined in split.yaml
    Boolean result = client.getBooleanValue("my_feature", false);
    assertTrue(result);

    // if we override the evaluation context for this check to use a different key,
    // this should take priority, and therefore we should receive a treatment of off
    EvaluationContext evaluationContext = new MutableContext("randomKey");
    result = client.getBooleanValue("my_feature", true, evaluationContext);
    assertFalse(result);
  }

  @Test
  public void getStringSplitTest() {
    String result = client.getStringValue("some_other_feature", "on");
    assertEquals("off", result);
  }

  @Test
  public void getIntegerSplitTest() {
    Integer result = client.getIntegerValue("int_feature", 0);
    assertEquals(32, result);
  }

  @Test
  public void getObjectSplitTest() {
    Value result = client.getObjectValue("obj_feature", new Value());
    assertEquals(mapToValue(Map.of("key", new Value("value"))), result);
  }

  @Test
  public void getDoubleSplitTest() {
    Double result = client.getDoubleValue("int_feature", 0D);
    assertEquals(32D, result);
  }

  @Test
  public void getMetadataNameTest() {
    assertEquals("Split Client", client.getMetadata().getName());
    assertEquals("Split", openFeatureAPI.getProviderMetadata().getName());
  }

  @Test
  public void getBooleanDetailsTest() {
    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("some_other_feature", true);
    assertEquals("some_other_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertFalse(details.getValue());
    // the flag has a treatment of "off", this is returned as a value of false but the variant is still "off"
    assertEquals("off", details.getVariant());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getIntegerDetailsTest() {
    FlagEvaluationDetails<Integer> details = client.getIntegerDetails("int_feature", 0);
    assertEquals("int_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertEquals(32, details.getValue());
    // the flag has a treatment of "32", this is resolved to an integer but the variant is still "32"
    assertEquals("32", details.getVariant());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getStringWithDetailsTest() {
    FlagEvaluationDetails<String> details = client.getStringDetails("my_feature", "key");
    assertEquals("my_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertEquals("on", details.getValue());
    assertEquals("on", details.getVariant());
    assertEquals("{\"desc\" : \"this applies only to ON treatment\"}", details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getStringWithoutDetailsTest() {
    FlagEvaluationDetails<String> details = client.getStringDetails("some_other_feature", "blah");
    assertEquals("some_other_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertEquals("off", details.getValue());
    // the flag has a treatment of "off", since this is a string the variant is the same as the value
    assertEquals("off", details.getVariant());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getObjectDetailsTest() {
    FlagEvaluationDetails<Value> details = client.getObjectDetails("obj_feature", new Value());
    assertEquals("obj_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertEquals(mapToValue(Map.of("key", new Value("value"))), details.getValue());
    // the flag's treatment is stored as a string, and the variant is that raw string
    assertEquals("{\"key\": \"value\"}", details.getVariant());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getDoubleDetailsTest() {
    FlagEvaluationDetails<Double> details = client.getDoubleDetails("int_feature", 0D);
    assertEquals("int_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH.name(), details.getReason());
    assertEquals(32D, details.getValue());
    // the flag has a treatment of "32", this is resolved to a double but the variant is still "32"
    assertEquals("32", details.getVariant());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getErrorCode());
  }

  @Test
  public void getBooleanFailTest() {
    // attempt to fetch an object treatment as a Boolean. Should result in the default
    Boolean value = client.getBooleanValue("obj_feature", false);
    assertFalse(value);

    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("obj_feature", false);
    assertFalse(details.getValue());
    assertEquals(ErrorCode.PARSE_ERROR, details.getErrorCode());
    assertEquals(Reason.ERROR.name(), details.getReason());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getVariant());
  }

  @Test
  public void getIntegerFailTest() {
    // attempt to fetch an object treatment as an integer. Should result in the default
    Integer value = client.getIntegerValue("obj_feature", 10);
    assertEquals(10, value);

    FlagEvaluationDetails<Integer> details = client.getIntegerDetails("obj_feature", 10);
    assertEquals(10, details.getValue());
    assertEquals(ErrorCode.GENERAL, details.getErrorCode());
    assertEquals(Reason.ERROR.name(), details.getReason());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getVariant());
  }

  @Test
  public void getDoubleFailTest() {
    // attempt to fetch an object treatment as a double. Should result in the default
    Double value = client.getDoubleValue("obj_feature", 10D);
    assertEquals(10D, value);

    FlagEvaluationDetails<Double> details = client.getDoubleDetails("obj_feature", 10D);
    assertEquals(10D, details.getValue());
    assertEquals(ErrorCode.GENERAL, details.getErrorCode());
    assertEquals(Reason.ERROR.name(), details.getReason());
    assertNull(details.getFlagMetadata().getString("config"));
    assertNull(details.getVariant());
  }

  @Test
  public void getObjectFailTest() {
    // attempt to fetch an int as an object. Should result in the default
    Value defaultValue = mapToValue(Map.of("foo", new Value("bar")));
    Value value = client.getObjectValue("int_feature", defaultValue);
    assertEquals(defaultValue, value);

    FlagEvaluationDetails<Value> details = client.getObjectDetails("int_feature", defaultValue);
    assertEquals(defaultValue, details.getValue());
    assertEquals(ErrorCode.PARSE_ERROR, details.getErrorCode());
    assertEquals(Reason.ERROR.name(), details.getReason());
    assertNull(details.getVariant());
  }

  @Test
  public void destroySplitClientTest() {
    assertEquals("32", splitClient.getTreatment("key","int_feature"));
    openFeatureAPI.shutdown();
    assertEquals("control", splitClient.getTreatment("key","int_feature"));
  }

  private Value mapToValue(Map<String, Value> map) {
    return new Value(new MutableStructure(map));
  }
}
