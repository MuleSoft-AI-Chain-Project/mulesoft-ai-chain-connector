package org.mule.extension.mulechain.internal.llm.config;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.llm.LangchainLLMConfiguration;

import static org.mule.extension.mulechain.internal.util.JsonUtils.readConfigFile;

public class FileConfigExtractor implements ConfigExtractor {

  private JSONObject llmConfig;

  public FileConfigExtractor(LangchainLLMConfiguration configuration) {
    JSONObject config = readConfigFile(configuration.getFilePath());
    if (config != null) {
      llmConfig = config.getJSONObject(configuration.getLlmType());
    }
  }

  @Override
  public String extractValue(String key) {
    return llmConfig.getString(key);
  }

}
