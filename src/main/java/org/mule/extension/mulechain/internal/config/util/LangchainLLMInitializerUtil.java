/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.config.util;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;

import static java.time.Duration.ofSeconds;

public final class LangchainLLMInitializerUtil {

  private LangchainLLMInitializerUtil() {}

  public static OpenAiChatModel createOpenAiChatModel(ConfigExtractor configExtractor, LangchainLLMConfiguration configuration) {
    String openaiApiKey = configExtractor.extractValue("OPENAI_API_KEY");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return OpenAiChatModel.builder()
        .apiKey(openaiApiKey)
        .modelName(configuration.getModelName())
        .maxTokens(configuration.getMaxTokens())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .logRequests(true)
        .logResponses(true)
        .build();

  }

  public static OpenAiChatModel createGroqOpenAiChatModel(ConfigExtractor configExtractor,
                                                          LangchainLLMConfiguration configuration) {
    String groqApiKey = configExtractor.extractValue("GROQ_API_KEY");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return OpenAiChatModel.builder()
        .baseUrl("https://api.groq.com/openai/v1")
        .apiKey(groqApiKey)
        .modelName(configuration.getModelName())
        .maxTokens(configuration.getMaxTokens())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .logRequests(true)
        .logResponses(true)
        .build();

  }


  public static MistralAiChatModel createMistralAiChatModel(ConfigExtractor configExtractor,
                                                            LangchainLLMConfiguration configuration) {
    String mistralAiApiKey = configExtractor.extractValue("MISTRAL_AI_API_KEY");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return MistralAiChatModel.builder()
        //.apiKey(configuration.getLlmApiKey())
        .apiKey(mistralAiApiKey)
        .modelName(configuration.getModelName())
        .maxTokens(configuration.getMaxTokens())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .logRequests(true)
        .logResponses(true)
        .build();
  }

  public static OllamaChatModel createOllamaChatModel(ConfigExtractor configExtractor, LangchainLLMConfiguration configuration) {
    String ollamaBaseUrl = configExtractor.extractValue("OLLAMA_BASE_URL");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return OllamaChatModel.builder()
        //.baseUrl(configuration.getLlmApiKey())
        .baseUrl(ollamaBaseUrl)
        .modelName(configuration.getModelName())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .build();
  }


  public static AnthropicChatModel createAnthropicChatModel(ConfigExtractor configExtractor,
                                                            LangchainLLMConfiguration configuration) {
    String anthropicApiKey = configExtractor.extractValue("ANTHROPIC_API_KEY");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return AnthropicChatModel.builder()
        //.apiKey(configuration.getLlmApiKey())
        .apiKey(anthropicApiKey)
        .modelName(configuration.getModelName())
        .maxTokens(configuration.getMaxTokens())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .logRequests(true)
        .logResponses(true)
        .build();
  }


  public static AzureOpenAiChatModel createAzureOpenAiChatModel(ConfigExtractor configExtractor,
                                                                LangchainLLMConfiguration configuration) {
    String azureOpenaiKey = configExtractor.extractValue("AZURE_OPENAI_KEY");
    String azureOpenaiEndpoint = configExtractor.extractValue("AZURE_OPENAI_ENDPOINT");
    String azureOpenaiDeploymentName = configExtractor.extractValue("AZURE_OPENAI_DEPLOYMENT_NAME");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    return AzureOpenAiChatModel.builder()
        .apiKey(azureOpenaiKey)
        .endpoint(azureOpenaiEndpoint)
        .deploymentName(azureOpenaiDeploymentName)
        .maxTokens(configuration.getMaxTokens())
        .temperature(configuration.getTemperature())
        .timeout(ofSeconds(durationInSec))
        .logRequestsAndResponses(true)
        .build();
  }
}
