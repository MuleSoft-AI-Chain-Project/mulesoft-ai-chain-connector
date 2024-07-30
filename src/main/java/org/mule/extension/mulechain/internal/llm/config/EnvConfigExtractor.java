package org.mule.extension.mulechain.internal.llm.config;

public class EnvConfigExtractor implements ConfigExtractor {

  @Override
  public String extractValue(String key) {
    return System.getenv(key).replace("\n", "").replace("\r", "");
  }

}
