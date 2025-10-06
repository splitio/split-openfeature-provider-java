package io.split.openfeature.utils;

import java.util.Map;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.exceptions.ParseError;
import io.split.client.utils.Json;

public class Serialization {

  private Serialization() {
  }

  public static Map<String, Object> stringToMap(final String obj) {
    try {
      return Json.fromJson(obj, Map.class);
    } catch (Exception e) {
      throw new ParseError(ErrorCode.PARSE_ERROR.name());
    }
  }
}
