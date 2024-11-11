/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import dev.langchain4j.model.chat.ChatLanguageModel;

import static org.mule.extension.mulechain.internal.helpers.ResponseHelper.createLLMResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mule.extension.mulechain.api.metadata.LLMResponseAttributes;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.extension.mulechain.internal.error.provider.AiServiceErrorTypeProvider;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainLLMOperations.class);
  private static final String URL_BASE = "https://api.openai.com/v1/moderations";
  private static final String MODERATION_MODEL = "omni-moderation-latest";

  interface Assistant {

    Result<String> chat(String userMessage);
  }

  /**
   * Implements a simple Chat agent to enable chat with the LLM
   * 
   * @param configuration Refers to the configuration object
   * @param prompt User defined prompt query
   * @return Returns the corresponding response as returned by the LLM
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("CHAT-answer-prompt")
  @Throws(AiServiceErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> answerPromptByModelName(@Config LangchainLLMConfiguration configuration,
                                                                                                                             @Content String prompt) {
    // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create
    try {
      LOGGER.debug("Chat Answer Prompt Operation called with prompt: {}", prompt);
      ChatLanguageModel model = configuration.getModel();
      Assistant assistant = AiServices.create(Assistant.class, model);
      Result<String> answer = assistant.chat(prompt);
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, answer.content());
      LOGGER.debug("Chat Answer Prompt Operation completed with response: {}", answer.content());
      return createLLMResponse(jsonObject.toString(), answer, new HashMap<>());
    } catch (Exception e) {
      throw new ModuleException("Unable to respond with the chat provided", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }

  /**
   * Helps in defining an AI Agent configured with a prompt template
   *
   * @param configuration Refers to the configuration object
   * @param dataset Refers to the user query to be acted upon
   * @param template Refers to sample template used by LLM to respond adequately to the user queries
   * @param instructions This provides the LLM on how to understand and respond to the user queries
   * @return Returns the corresponding response as returned by the LLM
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("AGENT-define-prompt-template")
  @Throws(AiServiceErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> definePromptTemplate(@Config LangchainLLMConfiguration configuration,
                                                                                                                          @Content(
                                                                                                                              primary = true) String dataset,
                                                                                                                          @Content String template,
                                                                                                                          @Content String instructions) {

    try {
      LOGGER.debug("Agent Define Prompt Template Operation called with prompt: {}, template: {} & instruction: {}", dataset,
                   template, instructions);
      ChatLanguageModel model = configuration.getModel();

      PromptTemplate promptTemplate = PromptTemplate.from(template + System.lineSeparator() + "Instructions: {{instructions}}"
          + System.lineSeparator() + "Dataset: {{dataset}}");

      Map<String, Object> variables = new HashMap<>();
      variables.put(MuleChainConstants.INSTRUCTIONS, instructions);
      variables.put(MuleChainConstants.DATASET, dataset);

      Prompt prompt = promptTemplate.apply(variables);

      Assistant assistant = AiServices.create(Assistant.class, model);

      Result<String> answer = assistant.chat(prompt.text());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, answer.content());
      LOGGER.debug("Agent Define Prompt Template Operation completed with response: {}", answer.content());
      return createLLMResponse(jsonObject.toString(), answer, new HashMap<>());
    } catch (Exception e) {
      throw new ModuleException("Unable to reply with the correct prompt template", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }

  /**
   * Supporting ENUM and Interface for Sentiments
   */
  enum Sentiment {

    VERY_POSITIVE(0.75, 1.0), POSITIVE(0.5, 0.75), SLIGHTLY_POSITIVE(0.1, 0.5), NEUTRAL(-0.1, 0.1), SLIGHTLY_NEGATIVE(-0.5,
        -0.1), NEGATIVE(-0.75, -0.5), VERY_NEGATIVE(-1.0, -0.75);

    private final double lowerBound;
    private final double upperBound;

    Sentiment(double lowerBound, double upperBound) {
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
    }

    /**
     * Maps a floating-point score to a sentiment category.
     *
     * @param score The sentiment score
     * @return The corresponding sentiment category
     */
    public static Sentiment fromScore(double score) {
      for (Sentiment sentiment : values()) {
        if (score >= sentiment.lowerBound && score <= sentiment.upperBound) {
          return sentiment;
        }
      }
      return NEUTRAL; // Default to NEUTRAL if no match found
    }
  }

  interface SentimentAnalyzer {

    @UserMessage("Analyze sentiment of {{it}}")
    Result<Double> analyzeSentimentOf(String text); // Returning a floating-point score
  }

  /**
   * Analyzes the sentiment of the user data and returns both the sentiment score and category, and also provides a chat answer
   * based on the sentiment analysis.
   *
   * @param configuration Refers to the configuration object
   * @param data Refers to the user input which needs to be analyzed
   * @return Returns the response with both sentiment score, category, and chat reply
   */
  @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
  @Alias("SENTIMENT-analyze")
  @Throws(AiServiceErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> extractSentiments(
                                                                                                                       @Config LangchainLLMConfiguration configuration,
                                                                                                                       @Content String data) {

    LOGGER.debug("Sentiment Analyze Operation initiated with input data: {}", data);

    try {
      // Fetch the language model from the configuration
      ChatLanguageModel model = configuration.getModel();

      // Create an instance of SentimentAnalyzer using the language model
      SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

      // Analyze sentiment of the provided data and get a floating-point score between -1 and 1
      Result<Double> sentimentResult = sentimentAnalyzer.analyzeSentimentOf(data);
      double sentimentScore = sentimentResult.content(); // The dynamic sentiment score
      LOGGER.info("Sentiment analyzed with score: {}", sentimentScore);

      // Map the score to a sentiment category
      Sentiment sentimentCategory = Sentiment.fromScore(sentimentScore);
      LOGGER.info("Mapped sentiment category: {}", sentimentCategory);

      // Prepare the JSON response with both the sentiment score and category
      String jsonResponse = createSentimentResponse(sentimentScore, sentimentCategory);

      // Get a response based on the input data provided (instead of using sentiment category)
      String chatPrompt = "Respond to the following input briefly:" + data;

      // Call the answerPromptByModelName method to get the chat response based on the input data
      org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> chatResponseResult =
          answerPromptByModelName(configuration, chatPrompt);

      InputStream chatResponseStream = chatResponseResult.getOutput();

      // Convert InputStream to String
      String chatResponse = convertInputStreamToString(chatResponseStream);

      // Combine the responses into a JSON object
      JSONObject combinedResponse = new JSONObject(jsonResponse);
      combinedResponse.put("chatResponse", chatResponse); // Adding chat response to the JSON

      // Return the final result encapsulating the combined response and attributes
      return createLLMResponse(combinedResponse.toString(), sentimentResult, new HashMap<>());

    } catch (IllegalArgumentException ex) {
      LOGGER.error("Invalid input provided for sentiment analysis: {}", ex.getMessage());
      throw new ModuleException("Invalid input for sentiment analysis", MuleChainErrorType.AI_SERVICES_FAILURE, ex);
    } catch (Exception ex) {
      LOGGER.error("Error during sentiment analysis: {}", ex.getMessage(), ex);
      throw new ModuleException("Failed to analyze sentiment", MuleChainErrorType.AI_SERVICES_FAILURE, ex);
    }
  }

  /**
   * Helper method to convert InputStream to String.
   *
   * @param inputStream The InputStream to convert
   * @return The String representation of the InputStream
   */
  private String convertInputStreamToString(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    while ((line = reader.readLine()) != null) {
      resultStringBuilder.append(line);
    }
    return resultStringBuilder.toString();
  }

  /**
   * Helper method to create a JSON response string for sentiment analysis.
   *
   * @param sentimentScore The floating-point score of the analyzed sentiment (-1 to 1)
   * @param sentimentCategory The sentiment category (VERY_POSITIVE, POSITIVE, etc.)
   * @return A JSON string containing the sentiment score and category
   */
  private String createSentimentResponse(double sentimentScore, Sentiment sentimentCategory) {
    JSONObject jsonResponse = new JSONObject();
    jsonResponse.put(MuleChainConstants.SENTIMENT_SCORE, sentimentScore); // Sentiment score as floating-point
    jsonResponse.put(MuleChainConstants.SENTIMENT_CATEGORY, sentimentCategory.name()); // Sentiment category as string
    return jsonResponse.toString();
  }

  /**
   * Use OpenAI Moderation models to moderate the input (any, from user or llm)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("Toxicity-detection")
  @Throws(AiServiceErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> moderateInput(@Config LangchainLLMConfiguration configuration,
                                                                                                                   String input) {
    try {
      JSONObject payload = new JSONObject();
      payload.put("model", MODERATION_MODEL);
      payload.put("input", input);
      ConfigExtractor configExtractor = configuration.getConfigExtractor();
      String openaiApiKey = configExtractor.extractValue("OPENAI_API_KEY");

      String response = executeREST(openaiApiKey, payload.toString());
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, new JSONObject(response));

      LOGGER.debug("Toxicity detection result {}", response);
      Result<String> answer = Result.<String>builder()
          .content(response)
          .tokenUsage(null)
          .build();;
      return createLLMResponse(jsonObject.toString(), answer, new HashMap<>());
    } catch (Exception e) {
      throw new ModuleException("Unable to perform toxicity detection", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }

  private static HttpURLConnection getConnectionObject(URL url, String apiKey) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
    conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    return conn;
  }

  private static String executeREST(String apiKey, String payload) {

    try {
      URL url = new URL(URL_BASE);
      HttpURLConnection conn = getConnectionObject(url, apiKey);

      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader br = new BufferedReader(
                                                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          return response.toString();
        }
      } else {
        return "Error: " + responseCode;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Exception occurred: " + e.getMessage();
    }

  }



}
