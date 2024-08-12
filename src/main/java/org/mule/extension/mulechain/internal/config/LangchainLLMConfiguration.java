/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.mule.extension.mulechain.internal.exception.config.ConfigValidationException;
import org.mule.extension.mulechain.internal.operation.LangchainEmbeddingStoresOperations;
import org.mule.extension.mulechain.internal.operation.LangchainImageModelsOperations;
import org.mule.extension.mulechain.internal.llm.type.LangchainLLMType;
import org.mule.extension.mulechain.internal.llm.ConfigTypeProvider;
import org.mule.extension.mulechain.internal.config.util.LangchainLLMInitializerUtil;
import org.mule.extension.mulechain.internal.operation.LangchainLLMOperations;
import org.mule.extension.mulechain.internal.llm.LangchainLLMModelNameProvider;
import org.mule.extension.mulechain.internal.llm.LangchainLLMTypeProvider;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;
import org.mule.extension.mulechain.internal.llm.config.ConfigType;
import org.mule.extension.mulechain.internal.llm.config.EnvConfigExtractor;
import org.mule.extension.mulechain.internal.llm.config.FileConfigExtractor;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "llm-configuration")
@Operations({LangchainLLMOperations.class, LangchainEmbeddingStoresOperations.class, LangchainImageModelsOperations.class})
public class LangchainLLMConfiguration implements Initialisable {

  private static final Map<LangchainLLMType, BiFunction<ConfigExtractor, LangchainLLMConfiguration, ChatLanguageModel>> llmMap;
  private static final Map<ConfigType, Function<LangchainLLMConfiguration, ConfigExtractor>> configExtractorMap;

  static {
    configExtractorMap = new HashMap<>();
    configExtractorMap.put(ConfigType.ENV_VARIABLE, (configuration) -> new EnvConfigExtractor());
    configExtractorMap.put(ConfigType.CONFIG_JSON, FileConfigExtractor::new);

    llmMap = new HashMap<>();
    llmMap.put(LangchainLLMType.OPENAI, (LangchainLLMInitializerUtil::createOpenAiChatModel));
    llmMap.put(LangchainLLMType.GROQAI_OPENAI, (LangchainLLMInitializerUtil::createGroqOpenAiChatModel));
    llmMap.put(LangchainLLMType.MISTRAL_AI, (LangchainLLMInitializerUtil::createMistralAiChatModel));
    llmMap.put(LangchainLLMType.OLLAMA, (LangchainLLMInitializerUtil::createOllamaChatModel));
    llmMap.put(LangchainLLMType.ANTHROPIC, (LangchainLLMInitializerUtil::createAnthropicChatModel));
    llmMap.put(LangchainLLMType.AZURE_OPENAI, (LangchainLLMInitializerUtil::createAzureOpenAiChatModel));
  }

  @Parameter
  @Placement(order = 1, tab = Placement.DEFAULT_TAB)
  @Optional(defaultValue = "OPENAI")
  @DisplayName("LLM type")
  @OfValues(LangchainLLMTypeProvider.class)
  private String llmType = "OPENAI";

  @Parameter
  @Placement(order = 2, tab = Placement.DEFAULT_TAB)
  @OfValues(ConfigTypeProvider.class)
  private String configType;

  @Parameter
  @Placement(order = 3, tab = Placement.DEFAULT_TAB)
  @Optional(defaultValue = "#[-]")
  private String filePath;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(LangchainLLMModelNameProvider.class)
  @Optional(defaultValue = "gpt-3.5-turbo")
  @Placement(order = 4)
  private String modelName = "gpt-3.5-turbo";

  @Parameter
  @Placement(order = 5)
  @Optional(defaultValue = "0.7")
  private double temperature = 0.7;

  @Parameter
  @Placement(order = 6)
  @Optional(defaultValue = "60")
  @DisplayName("LLM timeout")
  private int llmTimeout = 60;

  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Placement(order = 7)
  @DisplayName("LLM timeout unit")
  @Summary("Time unit to be used in the LLM Timeout")
  private TimeUnit llmTimeoutUnit = TimeUnit.SECONDS;

  @Parameter
  @Placement(order = 8)
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "500")
  private int maxTokens = 500;

  private ConfigExtractor configExtractor;

  private ChatLanguageModel model;

  public String getLlmType() {
    return llmType;
  }

  public String getConfigType() {
    return configType;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getModelName() {
    return modelName;
  }

  public double getTemperature() {
    return temperature;
  }

  public int getLlmTimeout() {
    return llmTimeout;
  }

  public TimeUnit getLlmTimeoutUnit() {
    return llmTimeoutUnit;
  }

  public int getMaxTokens() {
    return maxTokens;
  }

  public ConfigExtractor getConfigExtractor() {
    return configExtractor;
  }

  public ChatLanguageModel getModel() {
    return model;
  }

  private ChatLanguageModel createModel(ConfigExtractor configExtractor) {
    LangchainLLMType type = LangchainLLMType.valueOf(llmType);
    if (llmMap.containsKey(type)) {
      return llmMap.get(type).apply(configExtractor, this);
    }
    throw new ConfigValidationException("LLM Type not supported: " + llmType);
  }

  @Override
  public void initialise() throws InitialisationException {
    ConfigType config = ConfigType.fromValue(configType);
    if (configExtractorMap.containsKey(config)) {
      configExtractor = configExtractorMap.get(config).apply(this);
      model = createModel(configExtractor);
    } else {
      throw new ConfigValidationException("Config Type not supported: " + configType);
    }
  }
}
