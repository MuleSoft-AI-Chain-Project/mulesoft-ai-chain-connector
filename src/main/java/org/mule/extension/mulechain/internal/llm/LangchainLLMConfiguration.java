package org.mule.extension.mulechain.internal.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.mule.extension.mulechain.internal.embedding.stores.LangchainEmbeddingStoresOperations;
import org.mule.extension.mulechain.internal.image.models.LangchainImageModelsOperations;
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
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "llm-configuration")
@Operations({LangchainLLMOperations.class, LangchainEmbeddingStoresOperations.class, LangchainImageModelsOperations.class})
public class LangchainLLMConfiguration implements Initialisable {

  private static final Map<LLMType, BiFunction<ConfigExtractor, LangchainLLMConfiguration, ChatLanguageModel>> llmMap;
  private static final Map<ConfigType, Function<LangchainLLMConfiguration, ConfigExtractor>> configExtractorMap;

  static {
    configExtractorMap = new HashMap<>();
    configExtractorMap.put(ConfigType.ENV_VARIABLE, (configuration) -> new EnvConfigExtractor());
    configExtractorMap.put(ConfigType.CONFIG_JSON, FileConfigExtractor::new);

    llmMap = new HashMap<>();
    llmMap.put(LLMType.OPENAI, (LangchainLLMInitializerUtil::createOpenAiChatModel));
    llmMap.put(LLMType.GROQAI_OPENAI, (LangchainLLMInitializerUtil::createGroqOpenAiChatModel));
    llmMap.put(LLMType.MISTRAL_AI, (LangchainLLMInitializerUtil::createMistralAiChatModel));
    llmMap.put(LLMType.OLLAMA, (LangchainLLMInitializerUtil::createOllamaChatModel));
    llmMap.put(LLMType.ANTHROPIC, (LangchainLLMInitializerUtil::createAnthropicChatModel));
    llmMap.put(LLMType.AZURE_OPENAI, (LangchainLLMInitializerUtil::createAzureOpenAiChatModel));
  }

  @Parameter
  @OfValues(LangchainLLMTypeProvider.class)
  private String llmType;

  @Parameter
  @OfValues(LangchainLLMConfigType.class)
  private String configType;

  @Parameter
  private String filePath;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(LangchainLLMParameterModelNameProvider.class)
  @Optional(defaultValue = "gpt-3.5-turbo")
  private String modelName = "gpt-3.5-turbo";

  @Parameter
  @Optional(defaultValue = "0.7")
  private double temperature = 0.7;

  @Parameter
  @Optional(defaultValue = "60")
  @DisplayName("Duration in sec")
  private long durationInSeconds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "500")
  private int maxTokens;

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

  public long getDurationInSeconds() {
    return durationInSeconds;
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
    LLMType type = LLMType.valueOf(llmType);
    if (llmMap.containsKey(type)) {
      return llmMap.get(type).apply(configExtractor, this);
    }
    throw new IllegalArgumentException("LLM Type not supported: " + llmType);
  }

  @Override
  public void initialise() throws InitialisationException {
    ConfigType config = ConfigType.fromValue(configType);
    if (configExtractorMap.containsKey(config)) {
      configExtractor = configExtractorMap.get(config).apply(this);
      model = createModel(configExtractor);
    } else {
      throw new IllegalArgumentException("Config Type not supported: " + configType);
    }
  }
}
