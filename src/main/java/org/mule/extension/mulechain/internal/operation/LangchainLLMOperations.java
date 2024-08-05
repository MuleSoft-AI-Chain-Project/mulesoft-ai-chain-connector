/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import dev.langchain4j.model.chat.ChatLanguageModel;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
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
   * Implements a simple Chat agent
   */
  @MediaType(value = ANY, strict = false)
  @Alias("CHAT-answer-prompt")
  public String answerPromptByModelName(@Config LangchainLLMConfiguration configuration, String prompt) {
    // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create

    ChatLanguageModel model = configuration.getModel();

    Assistant assistant = AiServices.create(Assistant.class, model);

    Result<String> answer = assistant.chat(prompt);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, answer.content());
    jsonObject.put(MuleChainConstants.TOKEN_USAGE, JsonUtils.getTokenUsage(answer));
    return jsonObject.toString();
  }

  /**
   * Helps defining an AI Agent with a prompt template
   */
  @MediaType(value = ANY, strict = false)
  @Alias("AGENT-define-prompt-template")
  public String definePromptTemplate(@Config LangchainLLMConfiguration configuration, String template, String instructions,
                                     String dataset) {

    ChatLanguageModel model = configuration.getModel();

    PromptTemplate promptTemplate = PromptTemplate.from(template + System.lineSeparator() + "Instructions: {{instructions}}"
        + System.lineSeparator() + "Dataset: {{dataset}}");

    Map<String, Object> variables = new HashMap<>();
    variables.put(MuleChainConstants.INSTRUCTIONS, instructions);
    variables.put(MuleChainConstants.DATASET, dataset);

    Prompt prompt = promptTemplate.apply(variables);

    //String response = model.generate(prompt.text());
    Assistant assistant = AiServices.create(Assistant.class, model);

    Result<String> answer = assistant.chat(prompt.text());

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, answer.content());
    jsonObject.put(MuleChainConstants.TOKEN_USAGE, JsonUtils.getTokenUsage(answer));

    return jsonObject.toString();
  }

  /**
   * Supporting ENUM and Interface for Sentimetns
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
   * Example of a sentiment analyzer, which accepts text as input.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("SENTIMENT-analyze")
  public String extractSentiments(@Config LangchainLLMConfiguration configuration, String data) {

    ChatLanguageModel model = configuration.getModel();

    SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

    Result<Sentiment> sentiment = sentimentAnalyzer.analyzeSentimentOf(data);
    LOGGER.info("Analyzed sentiment: {}", sentiment); // POSITIVE

    boolean positive = sentimentAnalyzer.isPositive(data);
    LOGGER.info("Is sentiment positive: {}", positive); // false

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.SENTIMENT, sentiment.content());
    jsonObject.put(MuleChainConstants.IS_POSITIVE, positive);
    jsonObject.put(MuleChainConstants.TOKEN_USAGE, JsonUtils.getTokenUsage(sentiment));

    return jsonObject.toString();
  }



}
