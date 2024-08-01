/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
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
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Config Type: " + value));
  }

  public String getValue() {
    return value;
  }
}
