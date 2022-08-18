package io.split.openfeature;

import dev.openfeature.javasdk.Client;
import dev.openfeature.javasdk.ErrorCode;
import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.FlagEvaluationDetails;
import dev.openfeature.javasdk.OpenFeatureAPI;
import dev.openfeature.javasdk.Reason;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class uses the split.yaml file in src/test/resources for the flags and their treatments
 */
public class ClientTest {

  OpenFeatureAPI openFeatureAPI;
  Client client;

  @BeforeEach
  public void init() {
    openFeatureAPI = OpenFeatureAPI.getInstance();
    try {
      SplitClientConfig config = SplitClientConfig.builder().splitFile("src/test/resources/split.yaml").build();
      SplitClient client = SplitFactoryBuilder.build("localhost", config).client();
      openFeatureAPI.setProvider(new SplitProvider(client));
    } catch (URISyntaxException | IOException e) {
      System.out.println("Unexpected Exception occurred initializing Split Provider.");
    }
    client = openFeatureAPI.getClient("Split Client");
    EvaluationContext evaluationContext = new EvaluationContext();
    String targetingKey = "key";
    evaluationContext.setTargetingKey(targetingKey);
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

    String resultString = client.getStringValue(flagName, "blah");
    assertEquals("blah", resultString);

    Integer resultInt = client.getIntegerValue(flagName, 100);
    assertEquals(100, resultInt);

    Map<String, String> resultMap = client.getObjectValue(flagName, Map.of("default", "value"));
    assertEquals(Map.of("default", "value"), resultMap);
  }

  @Test
  public void missingTargetingKeyTest() {
    // Split requires a targeting key and should return the default treatment and throw an error if not provided
    client.setEvaluationContext(new EvaluationContext());
    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("non-existent-feature", false);
    assertFalse(details.getValue());
    assertEquals("TARGETING_KEY_MISSING", details.getErrorCode());
  }

  @Test
  public void getControlVariantNonExistentSplit() {
    // split returns a treatment = "control" if the flag is not found.
    // This should be interpreted by the Split provider to mean not found and therefore use the default value.
    // This control treatment should still be recorded as the variant.
    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("non-existent-feature", false);
    assertFalse(details.getValue());
    assertEquals("control", details.getVariant());
    assertEquals(ErrorCode.FLAG_NOT_FOUND.name(), details.getErrorCode());
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
    // this should take priority and therefore we should receive a treatment of off
    EvaluationContext evaluationContext = new EvaluationContext();
    evaluationContext.setTargetingKey("randomKey");
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
    Map<String, String> result = client.getObjectValue("obj_feature", new HashMap<>());
    assertEquals(Map.of("key", "value"), result);
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
    assertEquals(Reason.TARGETING_MATCH, details.getReason());
    assertFalse(details.getValue());
    // the flag has a treatment of "off", this is returned as a value of false but the variant is still "off"
    assertEquals("off", details.getVariant());
    assertNull(details.getErrorCode());
  }

  @Test
  public void getIntegerDetailsTest() {
    FlagEvaluationDetails<Integer> details = client.getIntegerDetails("int_feature", 0);
    assertEquals("int_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH, details.getReason());
    assertEquals(32, details.getValue());
    // the flag has a treatment of "32", this is resolved to an integer but the variant is still "32"
    assertEquals("32", details.getVariant());
    assertNull(details.getErrorCode());
  }

  @Test
  public void getStringDetailsTest() {
    FlagEvaluationDetails<String> details = client.getStringDetails("some_other_feature", "blah");
    assertEquals("some_other_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH, details.getReason());
    assertEquals("off", details.getValue());
    // the flag has a treatment of "off", since this is a string the variant is the same as the value
    assertEquals("off", details.getVariant());
    assertNull(details.getErrorCode());
  }

  @Test
  public void getObjectDetailsTest() {
    FlagEvaluationDetails<Map<String, String>> details = client.getObjectDetails("obj_feature", new HashMap<>());
    assertEquals("obj_feature", details.getFlagKey());
    assertEquals(Reason.TARGETING_MATCH, details.getReason());
    assertEquals(Map.of("key", "value"), details.getValue());
    // the flag's treatment is stored as a string, and the variant is that raw string
    assertEquals("{\"key\": \"value\"}", details.getVariant());
    assertNull(details.getErrorCode());
  }

  @Test
  public void getBooleanFailTest() {
    // attempt to fetch an object treatment as a boolean. Should result in the default
    Boolean value = client.getBooleanValue("obj_feature", false);
    assertFalse(value);

    FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("obj_feature", false);
    assertFalse(details.getValue());
    assertEquals(ErrorCode.PARSE_ERROR.name(), details.getErrorCode());
    assertEquals(Reason.ERROR, details.getReason());
    assertNull(details.getVariant());
  }

  @Test
  public void getIntegerFailTest() {
    // attempt to fetch an object treatment as an integer. Should result in the default
    Integer value = client.getIntegerValue("obj_feature", 10);
    assertEquals(10, value);

    FlagEvaluationDetails<Integer> details = client.getIntegerDetails("obj_feature", 10);
    assertEquals(10, details.getValue());
    assertEquals(ErrorCode.PARSE_ERROR.name(), details.getErrorCode());
    assertEquals(Reason.ERROR, details.getReason());
    assertNull(details.getVariant());
  }

  @Test
  public void getDoubleFailTest() {
    // attempt to fetch an object treatment as a double. Should result in the default
    Double value = client.getDoubleValue("obj_feature", 10D);
    assertEquals(10D, value);

    FlagEvaluationDetails<Double> details = client.getDoubleDetails("obj_feature", 10D);
    assertEquals(10D, details.getValue());
    assertEquals(ErrorCode.PARSE_ERROR.name(), details.getErrorCode());
    assertEquals(Reason.ERROR, details.getReason());
    assertNull(details.getVariant());
  }

  @Test
  public void getObjectFailTest() {
    // attempt to fetch a Map Object treatment as an Integer object. Due to Java generics there is no way to know of the
    // type mismatch until assignment here
    try {
      client.getObjectValue("obj_feature", 10);
    } catch (ClassCastException e) {

    } catch (Exception e) {
      fail("Unexpected exception occurred: ", e);
    }
  }
}
