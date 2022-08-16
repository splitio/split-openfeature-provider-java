package io.split.openfeature;

import dev.openfeature.javasdk.Client;
import dev.openfeature.javasdk.EvaluationContext;
import dev.openfeature.javasdk.OpenFeatureAPI;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    Boolean result = client.getBooleanValue("random-non-existent-feature", false);
    assertFalse(result);
    result = client.getBooleanValue("random-non-existent-feature", true);
    assertTrue(result);
  }

  @Test
  public void getBooleanSplitTest() {
    // This should be false as defined as "off" in the split.yaml
    Boolean result = client.getBooleanValue("some_other_feature", true);
    assertFalse(result);
  }

  @Test
  public void getBooleanSplitWithKeyTest() {
    // the key "key" was set in the before each. Therefore the treatment of true should be received
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
}
