package io.split.openfeature;


import dev.openfeature.javasdk.FlagEvaluationOptions;
import dev.openfeature.javasdk.ProviderEvaluation;
import dev.openfeature.javasdk.Reason;
import dev.openfeature.javasdk.exceptions.GeneralError;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SplitProviderTest {

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

    @Test
    @Disabled
    public void correctCreationTest() {
        // MUST FILL IN THIS APIKEY TO RUN TEST. REMOVE BEFORE COMMITTING.
        String apiKey = "";
        // YOU MUST ALSO ADD
        // `.endpoint("https://sdk.split-stage.io", "https://events.split-stage.io")`
        // to the SplitClientConfig.Builder in the SplitModule if the apiKey belongs to a stage apiKey
        SplitProvider splitProvider = new SplitProvider(apiKey);
        // split that exists
        ProviderEvaluation<String> response = splitProvider.getStringEvaluation(
                "colorLogoV3", "off", null, FlagEvaluationOptions.builder().build());
        assertEquals("on", response.getValue());
        assertEquals(Reason.SPLIT, response.getReason());
        // non-existent split (should use default)
        response = splitProvider.getStringEvaluation(
                "nonExistentFlag", "off", null, FlagEvaluationOptions.builder().build());
        assertEquals("off", response.getValue());
        assertEquals(Reason.SPLIT, response.getReason());
    }
}
