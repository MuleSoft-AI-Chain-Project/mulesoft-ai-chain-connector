package org.mule.extension.mulechain.internal.llm.config;

import java.util.Arrays;

public enum ConfigType {

  ENV_VARIABLE("Environment Variables"), CONFIG_JSON("Configuration Json");

  private final String value;

  ConfigType(String value) {
    this.value = value;
  }

  public static ConfigType fromValue(String value) {
    return Arrays.stream(ConfigType.values())
        .filter(configType -> configType.value.equals(value))
        .findFirst()
        .orElse(ENV_VARIABLE);
  }

  public String getValue() {
    return value;
  }
}
