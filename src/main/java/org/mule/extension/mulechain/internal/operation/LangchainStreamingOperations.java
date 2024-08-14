/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.extension.mulechain.internal.error.provider.AiServiceErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import static java.time.Duration.ofSeconds;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainStreamingOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainStreamingOperations.class);

  interface Assistant {

    TokenStream chat(String userMessage);
  }

  /**
   * Implements a simple Chat agent
   */
  @MediaType(value = ANY, strict = false)
  @Alias("CHAT-answer-prompt-w-stream")
  @Throws(AiServiceErrorTypeProvider.class)
  public InputStream answerPromptByModelNameStream(@Config LangchainLLMConfiguration configuration, String prompt) {
    String openaiApiKey = configuration.getConfigExtractor().extractValue("OPENAI_API_KEY");
    long durationInSec = configuration.getLlmTimeoutUnit().toSeconds(configuration.getLlmTimeout());
    try {
      StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
          .apiKey(openaiApiKey)
          .modelName(configuration.getModelName())
          .maxTokens(configuration.getMaxTokens())
          .temperature(configuration.getTemperature())
          .timeout(ofSeconds(durationInSec))
          .build();
      Assistant assistant = AiServices.create(Assistant.class, model);
      TokenStream tokenStream = assistant.chat(prompt);

      PipedOutputStream pipedOutputStream = new PipedOutputStream();
      PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);

      tokenStream.onNext(value -> {
        try {
          pipedOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new ModuleException("Error occurred while streaming output", MuleChainErrorType.STREAMING_FAILURE, e);
        }
      })
          .onComplete(response -> {
            try {
              pipedOutputStream.close();
            } catch (IOException e) {
              throw new ModuleException("Error occurred while closing the stream", MuleChainErrorType.STREAMING_FAILURE, e);
            }
          })
          .onError(throwable -> {
            throw new ModuleException("Exception occurred onError()", MuleChainErrorType.STREAMING_FAILURE, throwable);
          })
          .start();
      return pipedInputStream;
    } catch (Exception e) {
      throw new ModuleException("Unable to respond with the chat provided", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }



}
