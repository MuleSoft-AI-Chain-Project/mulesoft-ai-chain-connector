/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.llm.config;

import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.error.exception.config.ConfigValidationException;

import java.util.Arrays;
import java.util.function.Function;

public enum ConfigType {

  ENV_VARIABLE("Environment Variables", (configuration) -> new EnvConfigExtractor()), CONFIG_JSON("Configuration Json",
      FileConfigExtractor::new);

  private final String value;

  private final Function<LangchainLLMConfiguration, ConfigExtractor> configExtractorFunction;

  ConfigType(String value, Function<LangchainLLMConfiguration, ConfigExtractor> configExtractorFunction) {
    this.value = value;
    this.configExtractorFunction = configExtractorFunction;
  }

  public static ConfigType fromValue(String value) {
    return Arrays.stream(ConfigType.values())
        .filter(configType -> configType.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new ConfigValidationException("Unsupported Config Type: " + value));
  }

  public String getValue() {
    return value;
  }

  public Function<LangchainLLMConfiguration, ConfigExtractor> getConfigExtractorFunction() {
    return configExtractorFunction;
  }
}
