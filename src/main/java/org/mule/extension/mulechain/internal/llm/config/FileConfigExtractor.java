package org.mule.extension.mulechain.internal.llm.config;

import org.json.JSONObject;

import static org.mule.extension.mulechain.internal.util.JsonUtils.readConfigFile;

public class FileConfigExtractor implements ConfigExtractor {

  private JSONObject llmConfig;

  public FileConfigExtractor(String filePath, String llmType) {
    JSONObject config = readConfigFile(filePath);
    if (config != null) {
      llmConfig = config.getJSONObject(llmType);
    }
  }

  @Override
  public String extractValue(String key) {
    return llmConfig.getString(key);
  }

}
