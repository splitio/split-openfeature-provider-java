package io.split.openfeature.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Serialization {
  private static final Logger _log = LoggerFactory.getLogger(Serialization.class);

  public static <T> String serialize(T object) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(object);
  }

  public static <T> T deserialize(final String obj, final TypeReference<T> type) throws JsonProcessingException {
    T data = null;
    data = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .readValue(obj, type);
    return data;
  }
}
