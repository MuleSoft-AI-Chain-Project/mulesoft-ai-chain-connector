package org.mule.extension.mulechain.internal.llm.type;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;

public class ModerationModelType {

  private static final String URL_BASE_OPENAI = "https://api.openai.com/v1";
  private static final String URL_BASE_MISTRAL = "https://api.mistral.ai/v1";
  private static final String MODERATION_RESSOURCE = "/moderations";
  private static final String MODERATION_MODEL_OPENAI = "omni-moderation-latest";
  private static final String MODERATION_MODEL_MISTRAL = "mistral-moderation-latest";

  public static JSONObject moderationType(String input, LangchainLLMConfiguration configuration) {
    JSONObject resultObject = new JSONObject();
    JSONObject payload = new JSONObject();
    payload.put("input", input);
    String apiKey = "";
    String urlString = "";

    if ("OPENAI".equals(configuration.getLlmType())) {
      payload.put("model", MODERATION_MODEL_OPENAI);
      ConfigExtractor configExtractor = configuration.getConfigExtractor();
      apiKey = configExtractor.extractValue("OPENAI_API_KEY");
      urlString = URL_BASE_OPENAI + MODERATION_RESSOURCE;
    } else if ("MISTRAL_AI".equals(configuration.getLlmType())) {
      payload.put("model", MODERATION_MODEL_MISTRAL);
      ConfigExtractor configExtractor = configuration.getConfigExtractor();
      apiKey = configExtractor.extractValue("MISTRAL_AI_API_KEY");
      urlString = URL_BASE_MISTRAL + MODERATION_RESSOURCE;

    }

    resultObject.put("payload", payload);
    resultObject.put("url", urlString);
    resultObject.put("apiKey", apiKey);

    return resultObject;
  }
}
