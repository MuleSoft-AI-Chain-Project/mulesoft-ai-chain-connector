/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.llm.config;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;

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
