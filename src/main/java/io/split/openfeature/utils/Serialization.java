package io.split.openfeature.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.javasdk.ErrorCode;
import dev.openfeature.javasdk.exceptions.ParseError;

import java.util.Map;

public class Serialization {

  private Serialization() {
  }

  public static Map<String, Object> stringToMap(final String obj) {
    try {
      return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(obj, Map.class);
    } catch (JsonProcessingException e) {
      throw new ParseError(ErrorCode.PARSE_ERROR.name());
    }
  }
}
