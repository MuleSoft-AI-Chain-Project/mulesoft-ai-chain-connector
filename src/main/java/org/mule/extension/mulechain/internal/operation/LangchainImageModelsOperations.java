/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Config;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainImageModelsOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainImageModelsOperations.class);

  /**
   * Reads an image from a URL.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-read")
  public String readFromImage(@Config LangchainLLMConfiguration configuration, String data, String contextURL) {

    ChatLanguageModel model = configuration.getModel();

    UserMessage userMessage = UserMessage.from(
                                               TextContent.from(data),
                                               ImageContent.from(contextURL));

    Response<AiMessage> response = model.generate(userMessage);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, response.content().text());
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put(MuleChainConstants.INPUT_COUNT, response.tokenUsage().inputTokenCount());
    tokenUsage.put(MuleChainConstants.OUTPUT_COUNT, response.tokenUsage().outputTokenCount());
    tokenUsage.put(MuleChainConstants.TOTAL_COUNT, response.tokenUsage().totalTokenCount());
    jsonObject.put(MuleChainConstants.TOKEN_USAGE, tokenUsage);

    return jsonObject.toString();
  }

  /**
   * Generates an image based on the prompt in data
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-generate")
  public String drawImage(@Config LangchainLLMConfiguration configuration, String data) {
    ConfigExtractor configExtractor = configuration.getConfigExtractor();
    ImageModel model = OpenAiImageModel.builder()
        .modelName(configuration.getModelName())
        .apiKey(configExtractor.extractValue("OPENAI_API_KEY"))
        .build();

    Response<Image> response = model.generate(data);
    LOGGER.info("Generated Image: {}", response.content().url());

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, response.content().url());

    return jsonObject.toString();
  }
}
