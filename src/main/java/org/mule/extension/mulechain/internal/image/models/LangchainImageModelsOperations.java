package org.mule.extension.mulechain.internal.image.models;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.llm.LangchainLLMConfiguration;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Config;

import static org.mule.extension.mulechain.internal.util.JsonUtils.readConfigFile;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;

import java.net.URI;

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
   * Reads an image from an URL. 
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-read")
  public String readFromImage(String data, String contextURL, @Config LangchainLLMConfiguration configuration) {

    ChatLanguageModel model = configuration.getModel();

    UserMessage userMessage = UserMessage.from(
                                               TextContent.from(data),
                                               ImageContent.from(contextURL));

    Response<AiMessage> response = model.generate(userMessage);

    return response.content().text();
  }


  /**
   * Generates an image based on the prompt in data
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-generate")
  public URI drawImage(String data, @Config LangchainLLMConfiguration configuration) {
    ImageModel model = null;
    JSONObject config = readConfigFile(configuration.getFilePath());
    if (configuration.getConfigType().equals("Environment Variables")) {
      model = OpenAiImageModel.builder()
          .modelName(configuration.getModelName())
          .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
          .build();
    } else {
      JSONObject llmType = config.getJSONObject("OPENAI");
      String llmTypeKey = llmType.getString("OPENAI_API_KEY");
      model = OpenAiImageModel.builder()
          .modelName(configuration.getModelName())
          .apiKey(llmTypeKey.replace("\n", "").replace("\r", ""))
          .build();

    }
    /* ImageModel model = OpenAiImageModel.builder()
            .modelName(LangchainParams.getModelName())
            .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
            .build();
    */
    Response<Image> response = model.generate(data);
    LOGGER.info("Generated Image: {}", response.content().url());
    return response.content().url();
  }



}
