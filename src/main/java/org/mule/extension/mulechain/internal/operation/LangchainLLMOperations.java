/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import dev.langchain4j.model.chat.ChatLanguageModel;

import static org.mule.extension.mulechain.internal.helpers.ResponseHelper.createLLMResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mule.extension.mulechain.api.metadata.LLMResponseAttributes;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.extension.mulechain.internal.error.provider.AiServiceErrorTypeProvider;
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
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainLLMOperations.class);

  interface Assistant {

    Result<String> chat(String userMessage);
  }

  /**
   * Implements a simple Chat agent to enable chat with the LLM
   * @param configuration       Refers to the configuration object
   * @param prompt              User defined prompt query
   * @return                    Returns the corresponding response as returned by the LLM
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
   * @param configuration       Refers to the configuration object
   * @param dataset             Refers to the user query to be acted upon
   * @param template            Refers to sample template used by LLM to respond adequately to the user queries
   * @param instructions        This provides the LLM on how to understand and respond to the user queries
   * @return                    Returns the corresponding response as returned by the LLM
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
    POSITIVE, NEUTRAL, NEGATIVE;
  }

  interface SentimentAnalyzer {

    @UserMessage("Analyze sentiment of {{it}}")
    Result<Sentiment> analyzeSentimentOf(String text);

    @UserMessage("Does {{it}} have a positive sentiment?")
    boolean isPositive(String text);
  }

  /**
   * Analyzes the sentiment of the user data.
   *
   * @param configuration         Refers to the configuration object
   * @param data                  Refers to the user input which needs to be analyzed
   * @return                      Returns the response belonging to sentiment out of POSITIVE, NEUTRAL & NEGATIVE
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("SENTIMENT-analyze")
  @Throws(AiServiceErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> extractSentiments(@Config LangchainLLMConfiguration configuration,
                                                                                                                       @Content String data) {

    try {
      LOGGER.debug("Sentiment Analyze Operation called with data: {}", data);
      ChatLanguageModel model = configuration.getModel();
      SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);
      Result<Sentiment> sentiment = sentimentAnalyzer.analyzeSentimentOf(data);
      LOGGER.debug("Sentiment Analyze Operation completed with Analyzed sentiment: {}", sentiment); // POSITIVE

      boolean positive = sentimentAnalyzer.isPositive(data);
      LOGGER.debug("Is sentiment positive: {}", positive); // false

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, sentiment.content());

      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.IS_POSITIVE, String.valueOf(positive));

      return createLLMResponse(jsonObject.toString(), sentiment, attributes);
    } catch (Exception e) {
      throw new ModuleException("Unable to provide the correct sentiments", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }



}
