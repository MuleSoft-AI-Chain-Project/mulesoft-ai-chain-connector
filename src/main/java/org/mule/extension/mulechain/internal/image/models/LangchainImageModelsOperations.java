package org.mule.extension.mulechain.internal.image.models;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.llm.LangchainLLMParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import static java.time.Duration.ofSeconds;


/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainImageModelsOperations {


  private static JSONObject readConfigFile(String filePath) {
    Path path = Paths.get(filePath);
    if (Files.exists(path)) {
      try {
        String content = new String(Files.readAllBytes(path));
        return new JSONObject(content);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("File does not exist: " + filePath);
    }
    return null;
  }

  private static OpenAiChatModel createOpenAiChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName(LangchainParams.getModelName())
        .maxTokens(LangchainParams.getMaxToken())
        .temperature(LangchainParams.getTemperature())
        .timeout(ofSeconds(LangchainParams.getTimeoutInSeconds()))
        .logRequests(true)
        .logResponses(true)
        .build();

  }

  private static MistralAiChatModel createMistralAiChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
    return MistralAiChatModel.builder()
        //.apiKey(configuration.getLlmApiKey())
        .apiKey(apiKey)
        .modelName(LangchainParams.getModelName())
        .maxTokens(LangchainParams.getMaxToken())
        .temperature(LangchainParams.getTemperature())
        .timeout(ofSeconds(LangchainParams.getTimeoutInSeconds()))
        .logRequests(true)
        .logResponses(true)
        .build();
  }

  private static OllamaChatModel createOllamaChatModel(String baseURL, LangchainLLMParameters LangchainParams) {
    return OllamaChatModel.builder()
        //.baseUrl(configuration.getLlmApiKey())
        .baseUrl(baseURL)
        .modelName(LangchainParams.getModelName())
        .temperature(LangchainParams.getTemperature())
        .timeout(ofSeconds(LangchainParams.getTimeoutInSeconds()))
        .build();
  }


  private static AnthropicChatModel createAnthropicChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
    return AnthropicChatModel.builder()
        //.apiKey(configuration.getLlmApiKey())
        .apiKey(apiKey)
        .modelName(LangchainParams.getModelName())
        .maxTokens(LangchainParams.getMaxToken())
        .temperature(LangchainParams.getTemperature())
        .timeout(ofSeconds(LangchainParams.getTimeoutInSeconds()))
        .logRequests(true)
        .logResponses(true)
        .build();
  }


  private static AzureOpenAiChatModel createAzureOpenAiChatModel(String apiKey, String llmEndpoint, String deploymentName,
                                                                 LangchainLLMParameters LangchainParams) {
    return AzureOpenAiChatModel.builder()
        .apiKey(apiKey)
        .endpoint(llmEndpoint)
        .deploymentName(deploymentName)
        .maxTokens(LangchainParams.getMaxToken())
        .temperature(LangchainParams.getTemperature())
        .timeout(ofSeconds(LangchainParams.getTimeoutInSeconds()))
        .logRequestsAndResponses(true)
        .build();
  }



  private ChatLanguageModel createModel(LangchainLLMConfiguration configuration, LangchainLLMParameters LangchainParams) {
    ChatLanguageModel model = null;
    JSONObject config = readConfigFile(configuration.getFilePath());

    switch (configuration.getLlmType()) {
      case "OPENAI":
        if (configuration.getConfigType().equals("Environment Variables")) {
          model = createOpenAiChatModel(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
        } else {
          JSONObject llmType = config.getJSONObject("OPENAI");
          String llmTypeKey = llmType.getString("OPENAI_API_KEY");
          model = createOpenAiChatModel(llmTypeKey, LangchainParams);

        }
        break;
      case "MISTRAL_AI":
        if (configuration.getConfigType().equals("Environment Variables")) {
          model =
              createMistralAiChatModel(System.getenv("MISTRAL_AI_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
        } else {
          JSONObject llmType = config.getJSONObject("MISTRAL_AI");
          String llmTypeKey = llmType.getString("MISTRAL_AI_API_KEY");
          model = createMistralAiChatModel(llmTypeKey, LangchainParams);

        }
        break;
      case "OLLAMA":
        if (configuration.getConfigType().equals("Environment Variables")) {
          model = createOllamaChatModel(System.getenv("OLLAMA_BASE_URL").replace("\n", "").replace("\r", ""), LangchainParams);
        } else {
          JSONObject llmType = config.getJSONObject("OLLAMA");
          String llmTypeUrl = llmType.getString("OLLAMA_BASE_URL");
          model = createOllamaChatModel(llmTypeUrl, LangchainParams);

        }
        break;
      case "ANTHROPIC":
        if (configuration.getConfigType().equals("Environment Variables")) {
          model =
              createAnthropicChatModel(System.getenv("ANTHROPIC_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
        } else {
          JSONObject llmType = config.getJSONObject("ANTHROPIC");
          String llmTypeKey = llmType.getString("ANTHROPIC_API_KEY");
          model = createAnthropicChatModel(llmTypeKey, LangchainParams);
        }
        break;
      /* 			case "AWS_BEDROCK":
      				//String[] creds = configuration.getLlmApiKey().split("mulechain"); 
      				// For authentication, set the following environment variables:
        		// AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
       				model = BedrockAnthropicMessageChatModel.builder()
      						.region(Region.US_EAST_1)
      						.temperature(0.30f)
      						.maxTokens(300)
      						.model(LangchainParams.getModelName())
      						.maxRetries(1)
      						.build();
      				break;
       */ case "AZURE_OPENAI":
        if (configuration.getConfigType().equals("Environment Variables")) {
          model = createAzureOpenAiChatModel(System.getenv("AZURE_OPENAI_KEY").replace("\n", "").replace("\r", ""),
                                             System.getenv("AZURE_OPENAI_ENDPOINT").replace("\n", "").replace("\r", ""),
                                             System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME").replace("\n", "").replace("\r", ""),
                                             LangchainParams);
        } else {
          JSONObject llmType = config.getJSONObject("AZURE_OPENAI");
          String llmTypeKey = llmType.getString("AZURE_OPENAI_KEY");
          String llmEndpoint = llmType.getString("AZURE_OPENAI_ENDPOINT");
          String llmDeploymentName = llmType.getString("AZURE_OPENAI_DEPLOYMENT_NAME");
          model = createAzureOpenAiChatModel(llmTypeKey, llmEndpoint, llmDeploymentName, LangchainParams);
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported LLM type: " + configuration.getLlmType());
    }
    return model;
  }



  /**
   * Reads an image from an URL. 
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-read")
  public String readFromImage(String data, String contextURL, @Config LangchainLLMConfiguration configuration,
                              @ParameterGroup(name = "Additional properties") LangchainLLMParameters LangchainParams) {

    ChatLanguageModel model = createModel(configuration, LangchainParams);

    UserMessage userMessage = UserMessage.from(
                                               TextContent.from(data),
                                               ImageContent.from(contextURL));

    Response<AiMessage> response = model.generate(userMessage);



    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", response.content().text());
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put("inputCount", response.tokenUsage().inputTokenCount());
    tokenUsage.put("outputCount", response.tokenUsage().outputTokenCount());
    tokenUsage.put("totalCount", response.tokenUsage().totalTokenCount());
    jsonObject.put("tokenUsage", tokenUsage);


    return jsonObject.toString();

  }


  /**
   * Generates an image based on the prompt in data
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-generate")
  public String drawImage(String data, @Config LangchainLLMConfiguration configuration,
                          @ParameterGroup(name = "Additional properties") LangchainLLMParameters LangchainParams) {
    ImageModel model = null;
    JSONObject config = readConfigFile(configuration.getFilePath());
    if (configuration.getConfigType().equals("Environment Variables")) {
      model = OpenAiImageModel.builder()
          .modelName(LangchainParams.getModelName())
          .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
          .build();
    } else {
      JSONObject llmType = config.getJSONObject("OPENAI");
      String llmTypeKey = llmType.getString("OPENAI_API_KEY");
      model = OpenAiImageModel.builder()
          .modelName(LangchainParams.getModelName())
          .apiKey(llmTypeKey.replace("\n", "").replace("\r", ""))
          .build();

    }
    /* ImageModel model = OpenAiImageModel.builder()
            .modelName(LangchainParams.getModelName())
            .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
            .build();
    */
    Response<Image> response = model.generate(data);
    System.out.println(response.content().url());



    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", response.content().url());
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put("inputCount", response.tokenUsage().inputTokenCount());
    tokenUsage.put("outputCount", response.tokenUsage().outputTokenCount());
    tokenUsage.put("totalCount", response.tokenUsage().totalTokenCount());
    jsonObject.put("tokenUsage", tokenUsage);


    return jsonObject.toString();
  }



}
