package io.split.openfeature.impressions;

import dev.openfeature.javasdk.FlagEvaluationDetails;
import dev.openfeature.javasdk.Hook;
import dev.openfeature.javasdk.HookContext;
import io.split.integrations.azure.ImpressionListenerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class SplitImpressionsHook<T> implements Hook<T> {

    private static final Logger _log = LoggerFactory.getLogger(SplitImpressionsHook.class);
    private static final String DEFAULT_IMPRESSIONS_URL = "https://events.split-stage.io";

    private final ImpressionListenerImpl impressionListener;

    public SplitImpressionsHook(String apiToken) {
        this(apiToken, DEFAULT_IMPRESSIONS_URL);
    }

    public SplitImpressionsHook(String apiToken, String impressionsUrl) {
        ImpressionListenerProperties properties = new ImpressionListenerProperties();
        properties.setApiToken(apiToken);
        properties.setImpressionsUrl(impressionsUrl);
        this.impressionListener = new ImpressionListenerImpl(properties);
    }

    @Override
    public void after(HookContext<T> ctx, FlagEvaluationDetails<T> details, Map<String, Object> hints) {
        // FIXME: once eval context is defined we need to use it to get the split key
        String splitKey = ctx.getCtx().toString();
        String treatmentString = String.valueOf(details.getValue());
        String source = ctx.getProviderMetadata().getName();
        sendImpression(splitKey, ctx.getFlagKey(), treatmentString, source);
    }

    private void sendImpression(String splitKey, String flag, String treatment, String source) {
        try {
            impressionListener.handleImpression(splitKey, flag, treatment, source);
        } catch (Exception e) {
            _log.error("Error sending impression");
        }
    }
}
