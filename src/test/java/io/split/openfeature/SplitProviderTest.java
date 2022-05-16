package io.split.openfeature;


import dev.openfeature.javasdk.exceptions.GeneralError;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        SplitProvider splitProvider = new SplitProvider(apiKey);
    }
}
