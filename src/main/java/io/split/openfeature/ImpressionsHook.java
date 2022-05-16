package io.split.openfeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import dev.openfeature.javasdk.FlagEvaluationDetails;
import dev.openfeature.javasdk.Hook;
import dev.openfeature.javasdk.HookContext;
import io.codigo.dtos.KeyImpressionDTO;
import io.codigo.dtos.TestImpressionsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;


public class ImpressionsHook<T> extends Hook<T> {

    // TODO: we will need to pull dedupe logic in here for impressions

    private static final Logger _log = LoggerFactory.getLogger(ImpressionsHook.class);

    @Override
    public void after(HookContext<T> ctx, FlagEvaluationDetails<T> details, ImmutableMap<String, Object> hints) {
        // TODO: constcut key from details or hints...
        String key = null;
        // TODO constrcut label from reason?
        String label = "default rule";
        String treatmentString = String.valueOf(details.getValue());
        sendImpression(key, ctx.getFlagKey(), treatmentString, label);
    }


    private void sendImpression(String key, String flag, String treatment, String rule) {
        TestImpressionsDTO testImpressionsDTO = constructImpressionDTO(key, flag, treatment, rule);
        try {
            makePostRequest(testImpressionsDTO);
        } catch (Exception e) {
            _log.error("Error sending impression");
        }
    }

    private TestImpressionsDTO constructImpressionDTO(String key, String flag, String treatment, String label) {
        return TestImpressionsDTO.builder()
                .testName(flag)
                .keyImpressionsDTO(List.of(
                        KeyImpressionDTO.builder()
                                .keyId(key)
                                .treatment(treatment)
                                .time(System.currentTimeMillis())
                                .label(label)
                                .build()
                ))
                .build();
    }

    private String makePostRequest(TestImpressionsDTO testImpressionsDTO) throws Exception {
        String urlStr = null;
        Map<String, String> headers = Map.of();
        HttpURLConnection connection = null;
        String data = serialize(testImpressionsDTO);
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            setHeaders(headers, connection);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(data);
            wr.close();

            //Get Response
            int responseCode = connection.getResponseCode();
            _log.info(String.format("POST: %s - connection status: %s", urlStr, responseCode));
            if (responseCode >= 400 && responseCode < 500) {
                throw new RuntimeException("Response from " + url + ": " + responseCode);
            } else if (responseCode >= 500) {
                throw new RuntimeException("Response from " + url + ": " + responseCode);
            }

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }
            rd.close();
            return response.toString();
        } catch (IOException e) {
            throw new RuntimeException("Exception posting to " + urlStr, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void setHeaders(Map<String, String> headers, HttpURLConnection connection) {
        if (headers == null) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private String serialize(TestImpressionsDTO testImpressionsDTO) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(testImpressionsDTO);
        }
        catch (JsonProcessingException e) {
            _log.error("Error serializing impressions.");
            return null;
        }
    }
}
