package io.split.openfeature.impressions;

import com.google.common.collect.ImmutableMap;
import dev.openfeature.javasdk.FlagEvaluationDetails;
import dev.openfeature.javasdk.Hook;
import dev.openfeature.javasdk.HookContext;
import io.split.integrations.azure.ImpressionListenerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SplitImpressionsHook<T> extends Hook<T> {

    private static final Logger _log = LoggerFactory.getLogger(SplitImpressionsHook.class);

    ImpressionListenerProperties properties = null;
    ImpressionListenerImpl impressionListener = null;

    public SplitImpressionsHook() {}

    @Override
    public void after(HookContext<T> ctx, FlagEvaluationDetails<T> details, ImmutableMap<String, Object> hints) {
        if (properties == null) {
            setProperties(hints);
        }
        if (impressionListener == null) {
            impressionListener = new ImpressionListenerImpl(properties);
        }
        // FIXME: once eval context is defined we need to use it to get the split key
        String splitKey = ctx.getCtx().toString();
        String treatmentString = String.valueOf(details.getValue());
        String source = ctx.getProvider().getName();
        sendImpression(splitKey, ctx.getFlagKey(), treatmentString, source);
    }

    private void sendImpression(String splitKey, String flag, String treatment, String source) {
        try {
            impressionListener.handleImpression(splitKey, flag, treatment, source);
        } catch (Exception e) {
            _log.error("Error sending impression");
        }
    }

    private void setProperties(ImmutableMap<String, Object> hints) {
        properties = new ImpressionListenerProperties();
        properties.setApiToken((String) hints.get("apiToken"));
        properties.setImpressionsUrl((String) hints.get("impressionsUrl"));
    }
}
