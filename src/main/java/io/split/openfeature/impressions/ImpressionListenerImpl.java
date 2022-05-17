package io.split.openfeature.impressions;

import io.split.integrations.azure.ImpressionListenerProperties;
import io.split.integrations.azure.impressions.Impression;
import io.split.integrations.azure.impressions.ImpressionsManager;
import io.split.integrations.azure.impressions.ImpressionsManagerImpl;
import io.split.integrations.azure.impressions.ImpressionsStorage;
import io.split.integrations.azure.impressions.InMemoryImpressionsStorage;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URISyntaxException;
import java.util.List;


public class ImpressionListenerImpl {

    private transient ImpressionListenerProperties properties;

    private ImpressionsManager impressionsManager;

    public ImpressionListenerImpl(ImpressionListenerProperties properties) {
        this.properties = properties;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            ImpressionsStorage storage = new InMemoryImpressionsStorage(30000);
            impressionsManager = ImpressionsManagerImpl.instance(properties, httpClient, storage, storage);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public ImpressionListenerImpl getImpressionListenerImpl() {
        return this;
    }

    public void handleImpression(String key, String feature, String treatment, String source) {
        source = source.toUpperCase();
        String rule = "default rule"; // For now, always assume the default rule
        Impression i = new Impression(key, null, feature, treatment, System.currentTimeMillis(), rule, 0L, null, source);
        impressionsManager.track(List.of(i));
    }
}
