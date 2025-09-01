package io.split.openfeature;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.MutableTrackingEventDetails;
import dev.openfeature.sdk.MutableStructure;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.TrackingEventDetails;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.GeneralError;
import dev.openfeature.sdk.exceptions.OpenFeatureError;
import io.split.client.SplitClient;
import io.split.client.api.SplitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SplitProviderTest {

  EvaluationContext evaluationContext;
  String key;

  @Mock
  private SplitClient mockSplitClient;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);

    key = "key";
    evaluationContext = new MutableContext(key);
  }

  @Test
  public void shouldFailWithBadApiKeyTest() {
    String apiKey = "someKey";
    try {
      new SplitProvider(apiKey);
      fail("Should have thrown an exception");
    } catch (GeneralError e) {
      assertEquals("Error occurred initializing the client.", e.getMessage());
    } catch (Exception e) {
      fail("Unexpected exception occurred. Expected a GeneralError.", e);
    }
  }

  // *** Boolean eval tests ***

  @Test
  public void evalBooleanNullEmptyTest() {
    // if a treatment is null empty it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(null,""));

    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);
    assertFalse(response.getValue());

    response = splitProvider.getBooleanEvaluation(flagName, true, evaluationContext);
    assertTrue(response.getValue());

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("",""));

    response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);
    assertFalse(response.getValue());

    response = splitProvider.getBooleanEvaluation(flagName, true, evaluationContext);
    assertTrue(response.getValue());
  }

  @Test
  public void evalBooleanControlTest() {
    // if a treatment is "control" it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("control",""));

    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);
    assertFalse(response.getValue());

    response = splitProvider.getBooleanEvaluation(flagName, true, evaluationContext);
    assertTrue(response.getValue());
  }

  @Test
  public void evalBooleanTrueTest() {
    // treatment of "true" should eval to true
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("true",""));
    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);

    assertTrue(response.getValue());
  }

  @Test
  public void evalBooleanOnTest() {
    // "on" treatment should eval to true
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("on",""));
    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);

    assertTrue(response.getValue());
  }

  @Test
  public void evalBooleanFalseTest() {
    // "false" treatment should eval to false
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("false",""));
    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);

    assertFalse(response.getValue());
  }

  @Test
  public void evalBooleanOffTest() {
    // "off" treatment should eval to false
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("off",""));
    ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);

    assertFalse(response.getValue());
  }

  @Test
  public void evalBooleanErrorTest() {
    // any other random string other than on,off,true,false,control should throw an error
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("a random string",""));
    try {
      splitProvider.getBooleanEvaluation(flagName, false, evaluationContext);
      fail("Should have thrown an exception casting string to boolean");
    } catch (OpenFeatureError e) {
      assertEquals(ErrorCode.PARSE_ERROR, e.getErrorCode());
    } catch (Exception e) {
      fail("Unexpected exception occurred", e);
    }
  }

  // *** String eval tests ***

  @Test
  public void evalStringNullEmptyTest() {
    // if a treatment is null empty it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String defaultTreatment = "defaultTreatment";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(null,""));

    ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("",""));

    response = splitProvider.getStringEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalStringControlTest() {
    // "control" treatment should eval to default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String defaultTreatment = "defaultTreatment";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("control",""));

    ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalStringRegularTest() {
    // a string treatment should eval to itself
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String treatment = "treatment";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(treatment,""));

    ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, "defaultTreatment", evaluationContext);
    assertEquals(treatment, response.getValue());
  }

  // *** Int eval tests ***

  @Test
  public void evalIntNullEmptyTest() {
    // if a treatment is null empty it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    int defaultTreatment = 10;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(null,""));

    ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("",""));

    response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalIntControlTest() {
    // "control" treatment should eval to default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    int defaultTreatment = 10;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("control",""));

    ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalIntRegularTest() {
    // a parsable integer treatment should eval to that integer
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String numString = "50";
    int numInt = 50;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(numString,""));

    ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, 10, evaluationContext);
    assertEquals(numInt, response.getValue());
  }

  @Test
  public void evalIntErrorTest() {
    // an un-parsable integer treatment should throw an error
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String numString = "notAnInt";

    when(mockSplitClient.getTreatment(eq(key), eq(flagName), anyMap())).thenReturn(numString);

    try {
      splitProvider.getIntegerEvaluation(flagName, 10, evaluationContext);
      fail("Should have thrown an exception casting string to integer");
    } catch (OpenFeatureError e) {
      assertEquals(ErrorCode.GENERAL, e.getErrorCode());
    } catch (Exception e) {
      fail("Unexpected exception occurred", e);
    }
  }

  // *** Double eval tests ***

  @Test
  public void evalDoubleNullEmptyTest() {
    // if a treatment is null empty it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    double defaultTreatment = 10;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(null,""));

    ProviderEvaluation<Double> response = splitProvider.getDoubleEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("",""));

    response = splitProvider.getDoubleEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalDoubleControlTest() {
    // "control" treatment should eval to default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    double defaultTreatment = 10;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("control",""));

    ProviderEvaluation<Double> response = splitProvider.getDoubleEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalDoubleRegularTest() {
    // a parsable integer treatment should eval to that integer
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String numString = "50";
    double num = 50;

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(numString,""));

    ProviderEvaluation<Double> response = splitProvider.getDoubleEvaluation(flagName, 10D, evaluationContext);
    assertEquals(num, response.getValue());
  }

  @Test
  public void evalDoubleErrorTest() {
    // an un-parsable integer treatment should throw an error
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String numString = "notAnInt";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(numString,""));

    try {
      splitProvider.getDoubleEvaluation(flagName, 10D, evaluationContext);
      fail("Should have thrown an exception casting string to integer");
    } catch (OpenFeatureError e) {
      assertEquals(ErrorCode.GENERAL, e.getErrorCode());
    } catch (Exception e) {
      fail("Unexpected exception occurred", e);
    }
  }

  // *** Structure eval tests ***

  @Test
  public void evalStructureNullEmptyTest() {
    // if a treatment is null empty it should return the default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    Value defaultTreatment = mapToValue(Map.of("foo", new Value("bar")));

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(null,""));

    ProviderEvaluation<Value> response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("",""));

    response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalStructureControlTest() {
    // "control" treatment should eval to default treatment
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    Value defaultTreatment = mapToValue(Map.of("foo", new Value("bar")));

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult("control",""));

    ProviderEvaluation<Value> response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, evaluationContext);
    assertEquals(defaultTreatment, response.getValue());
  }

  @Test
  public void evalStructureRegularTest() {
    // an object treatment should eval to that object
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    Value treatment = mapToValue(Map.of("abc", new Value("def")));
    String treatmentAsString = "{\"abc\":\"def\"}";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(treatmentAsString,""));

    ProviderEvaluation<Value> response =
      splitProvider.getObjectEvaluation(flagName, mapToValue(Map.of("foo", new Value("bar"))), evaluationContext);
    assertEquals(treatment, response.getValue());
  }

  @Test
  public void evalStructureComplexTest() {
    // an object treatment should eval to that object
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    Instant instant = Instant.ofEpochMilli(1665698754828L);
    Value treatment = mapToValue(Map.of(
      "string", new Value("blah"),
      "int", new Value(10),
      "double", new Value(100D),
      "bool", new Value(true),
      "struct", mapToValue(Map.of(
          "foo", new Value("bar"),
          "baz", new Value(10),
          "innerMap", mapToValue(Map.of(
              "aa", new Value("bb"))))),
      "list", new Value(
        List.of(
          new Value(1),
          new Value(true),
          mapToValue(Map.of(
              "cc", new Value("dd")
            )),
          mapToValue(Map.of(
              "ee", new Value(1)
            )))),
      "dateTime", new Value(instant)
    ));
    String treatmentAsString = "{\"string\":\"blah\",\"int\":10,\"double\":100.0,\"bool\":true, \"struct\":{\"foo\":\"bar\",\"baz\":10,\"innerMap\":{\"aa\":\"bb\"}},\"list\":[1,true,{\"cc\":\"dd\"},{\"ee\":1}],\"dateTime\":\"2022-10-13T22:05:54.828Z\"}";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(treatmentAsString,""));

    ProviderEvaluation<Value> response =
      splitProvider.getObjectEvaluation(flagName, mapToValue(Map.of("foo", new Value("bar"))), evaluationContext);
    assertEquals(treatment, response.getValue());
  }

  @Test
  public void evalStructureErrorTest() {
    // a treatment that can not be converted to the required object should throw an error
    SplitProvider splitProvider = new SplitProvider(mockSplitClient);

    String flagName = "flagName";
    String treatment = "not an object";

    when(mockSplitClient.getTreatmentWithConfig(eq(key), eq(flagName), anyMap())).thenReturn(new SplitResult(treatment,""));

    try {
      splitProvider.getObjectEvaluation(flagName, mapToValue(Map.of("foo", new Value("bar"))), evaluationContext);
      fail("Should have thrown an exception casting string to an object");
    } catch (OpenFeatureError e) {
      assertEquals(ErrorCode.PARSE_ERROR.name(), e.getMessage());
    } catch (Exception e) {
      fail("Unexpected exception occurred", e);
    }
  }

  @Test
  public void trackWithDetailsTest() {
    SplitProvider provider = new SplitProvider(mockSplitClient);


    EvaluationContext ctx = new MutableContext(key).add("trafficType", "user");
    TrackingEventDetails details = new MutableTrackingEventDetails(42.5)
            .add("plan", new Value("pro"))
            .add("beta", new Value(true));

    when(mockSplitClient.track("key", "user", "purchase", 42.5, details.asObjectMap()))
            .thenReturn(true);

    provider.track("purchase", ctx, details);

    verify(mockSplitClient).track(
            eq(key), eq("user"), eq("purchase"), eq(42.5),
            argThat(m -> "pro".equals(m.get("plan")) && Boolean.TRUE.equals(m.get("beta"))));
    verifyNoMoreInteractions(mockSplitClient);
  }

  @Test
  public void trackTargetingKeyErrorTest() {
    // Tracking without targetingKey should throw error
    SplitProvider provider = new SplitProvider(mockSplitClient);
    EvaluationContext ctx = new MutableContext().add("trafficType", "user");

    assertThrows(dev.openfeature.sdk.exceptions.TargetingKeyMissingError.class,
            () -> provider.track("purchase", ctx, null));
    verifyNoInteractions(mockSplitClient);
  }

  @Test
  public void trackEventNameErrorTest() {
    // Tracking without eventName should throw error
    SplitProvider provider = new SplitProvider(mockSplitClient);
    EvaluationContext ctx = new MutableContext(key).add("trafficType", "user");

    GeneralError ex = assertThrows(GeneralError.class,
            () -> provider.track("  ", ctx, null)); // blank name
    assertTrue(ex.getMessage().toLowerCase().contains("eventname"));
    verifyNoInteractions(mockSplitClient);
  }

  @Test
  public void trackTrafficTypeErrorTest() {
    // Tracking without trafficType should throw error
    SplitProvider provider = new SplitProvider(mockSplitClient);
    EvaluationContext ctx = new MutableContext(key);

    GeneralError ex = assertThrows(GeneralError.class,
            () -> provider.track("purchase", ctx, null));
    assertTrue(ex.getMessage().toLowerCase().contains("traffictype"));
    verifyNoInteractions(mockSplitClient);
  }

  private Value mapToValue(Map<String, Value> map) {
    return new Value(new MutableStructure(map));
  }
}
