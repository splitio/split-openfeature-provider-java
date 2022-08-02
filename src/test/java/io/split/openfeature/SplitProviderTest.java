package io.split.openfeature;


import com.fasterxml.jackson.core.JsonProcessingException;
import dev.openfeature.javasdk.ProviderEvaluation;
import dev.openfeature.javasdk.exceptions.GeneralError;
import io.split.client.SplitClient;
import io.split.openfeature.utils.Serialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class SplitProviderTest {

    @BeforeEach
    private void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Mock
    private SplitClient mockSplitClient;

    @Test
    public void shouldFailWithBadApiKeyTest() {
        String apiKey = "someKey";
        try {
            SplitProvider splitProvider = new SplitProvider(apiKey);
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

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(null);

        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);
        assertFalse(response.getValue());

        response = splitProvider.getBooleanEvaluation(flagName, true, null, null);
        assertTrue(response.getValue());

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("");

        response = splitProvider.getBooleanEvaluation(flagName, false, null, null);
        assertFalse(response.getValue());

        response = splitProvider.getBooleanEvaluation(flagName, true, null, null);
        assertTrue(response.getValue());
    }

    @Test
    public void evalBooleanControlTest() {
        // if a treatment is "control" it should return the default treatment
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("control");

        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);
        assertFalse(response.getValue());

        response = splitProvider.getBooleanEvaluation(flagName, true, null, null);
        assertTrue(response.getValue());
    }

    @Test
    public void evalBooleanTrueTest() {
        // treatment of "true" should eval to true
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("true");
        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);

        assertTrue(response.getValue());
    }

    @Test
    public void evalBooleanOnTest() {
        // "on" treatment should eval to true
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("on");
        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);

        assertTrue(response.getValue());
    }

    @Test
    public void evalBooleanFalseTest() {
        // "false" treatment should eval to false
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("false");
        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);

        assertFalse(response.getValue());
    }

    @Test
    public void evalBooleanOffTest() {
        // "off" treatment should eval to false
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("off");
        ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);

        assertFalse(response.getValue());
    }

    @Test
    public void evalBooleanRandomStringTest() {
        // any other random string other than on,off,true,false,control should throw an error
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("a random string");
        try {
            ProviderEvaluation<Boolean> response = splitProvider.getBooleanEvaluation(flagName, false, null, null);
            fail("Should have thrown an exception casting string to integer");
        } catch (GeneralError e) {
            assertEquals("Error getting boolean evaluation", e.getMessage());
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

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(null);

        ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("");

        response = splitProvider.getStringEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalStringControlTest() {
        // "control" treatment should eval to default treatment
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        String defaultTreatment = "defaultTreatment";

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("control");

        ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalStringRegularTest() {
        // a string treatment should eval to itself
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        String treatment = "treatment";

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(treatment);

        ProviderEvaluation<String> response = splitProvider.getStringEvaluation(flagName, "defaultTreatment", null, null);
        assertEquals(treatment, response.getValue());
    }

    // *** Number eval tests ***

    @Test
    public void evalNumberNullEmptyTest() {
        // if a treatment is null empty it should return the default treatment
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        int defaultTreatment = 10;

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(null);

        ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("");

        response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalNumberControlTest() {
        // "control" treatment should eval to default treatment
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        int defaultTreatment = 10;

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("control");

        ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalNumberRegularTest() {
        // a parsable integer treatment should eval to that integer
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        String numString = "50";
        int numInt = 50;

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(numString);

        ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, 10, null, null);
        assertEquals(numInt, response.getValue());
    }

    @Test
    public void evalNumberErrorTest() {
        // an un-parsable integer treatment should throw an error
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        String numString = "notAnInt";

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(numString);

        try {
            ProviderEvaluation<Integer> response = splitProvider.getIntegerEvaluation(flagName, 10, null, null);
            fail("Should have thrown an exception casting string to integer");
        } catch (GeneralError e) {
            assertEquals("Error getting Integer evaluation", e.getMessage());
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
        Map<String, Object> defaultTreatment = Map.of("foo", "bar");

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(null);

        ProviderEvaluation<Map<String, Object>> response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("");

        response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalStructureControlTest() {
        // "control" treatment should eval to default treatment
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        Map<String, Object> defaultTreatment = Map.of("foo", "bar");

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn("control");

        ProviderEvaluation<Map<String, Object>> response = splitProvider.getObjectEvaluation(flagName, defaultTreatment, null, null);
        assertEquals(defaultTreatment, response.getValue());
    }

    @Test
    public void evalStructureRegularTest() {
        // an object treatment should eval to that object
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        Map<String, Object> treatment = Map.of("robert", "grassian");

        try {
            when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(Serialization.serialize(treatment));
        } catch (JsonProcessingException e) {
            fail("Unexpected exception occurred: ", e);
        }

        ProviderEvaluation<Map<String, Object>> response = splitProvider.getObjectEvaluation(flagName, Map.of("foo", "bar"), null, null);
        assertEquals(treatment, response.getValue());
    }

    @Test
    public void evalStructureErrorTest() {
        // a treatment that can not be converted to the required object should throw an error
        SplitProvider splitProvider = new SplitProvider(mockSplitClient);

        String flagName = "flagName";
        String treatment = "not an object";

        when(mockSplitClient.getTreatment(anyString(), eq(flagName), anyMap())).thenReturn(treatment);

        try {
            ProviderEvaluation<Map<String, Object>> response = splitProvider.getObjectEvaluation(flagName, Map.of("foo", "bar"), null, null);
            fail("Should have thrown an exception casting string to an object");
        } catch (GeneralError e) {
            assertEquals("Error getting Object evaluation", e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception occurred", e);
        }
    }
}
