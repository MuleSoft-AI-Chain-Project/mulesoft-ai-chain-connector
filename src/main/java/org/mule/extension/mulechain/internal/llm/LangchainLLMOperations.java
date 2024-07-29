package org.mule.extension.mulechain.internal.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.HashMap;
import java.util.Map;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainLLMOperations.class);

  /**
   * Implements a simple Chat agent
   */
  @MediaType(value = ANY, strict = false)
  @Alias("CHAT-answer-prompt")
  public String answerPromptByModelName(String prompt, @Config LangchainLLMConfiguration configuration) {
    // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create

    ChatLanguageModel model = configuration.getModel();
    return model.generate(prompt);
  }

  /**
   * Helps defining an AI Agent with a prompt template
   */
  @MediaType(value = ANY, strict = false)
  @Alias("AGENT-define-prompt-template")
  public String definePromptTemplate(String template, String instructions, String dataset,
                                     @Config LangchainLLMConfiguration configuration) {

    ChatLanguageModel model = configuration.getModel();

    PromptTemplate promptTemplate = PromptTemplate.from(template + System.lineSeparator() + "Instructions: {{instructions}}"
        + System.lineSeparator() + "Dataset: {{dataset}}");

    Map<String, Object> variables = new HashMap<>();
    variables.put("instructions", instructions);
    variables.put("dataset", dataset);

    Prompt prompt = promptTemplate.apply(variables);

    return model.generate(prompt.text());
  }



  /**
   * Supporting ENUM and Interface for Sentimetns
   */

  enum Sentiment {
    POSITIVE, NEUTRAL, NEGATIVE;
  }


  interface SentimentAnalyzer {

    @UserMessage("Analyze sentiment of {{it}}")
    Sentiment analyzeSentimentOf(String text);

    @UserMessage("Does {{it}} have a positive sentiment?")
    boolean isPositive(String text);
  }



  /**
   * Example of a sentiment analyzer, which accepts text as input.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("SENTIMENT-analyze")
  public Sentiment extractSentiments(String data, @Config LangchainLLMConfiguration configuration) {

    ChatLanguageModel model = configuration.getModel();


    SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

    Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf(data);
    LOGGER.info("Analyzed sentiment: {}", sentiment); // POSITIVE

    boolean positive = sentimentAnalyzer.isPositive(data);
    LOGGER.info("Is sentiment positive: {}", positive); // false

    return sentiment;
  }



}
