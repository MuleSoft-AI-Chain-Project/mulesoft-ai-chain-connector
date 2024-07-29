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

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "llm-configuration")
@Operations({LangchainLLMOperations.class, LangchainEmbeddingStoresOperations.class, LangchainImageModelsOperations.class})
public class LangchainLLMConfiguration implements Initialisable {

  private static final Map<LLMType, BiFunction<ConfigExtractor, LangchainLLMConfiguration, ChatLanguageModel>> llmMap;

  static {
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
  private String modelName;

  @Parameter
  @Optional(defaultValue = "0.7")
  private double temperature;

  @Parameter
  @Optional(defaultValue = "60")
  @DisplayName("Duration in sec")
  private long durationInSeconds;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "500")
  private Integer maxTokens;

  private ChatLanguageModel model;

  private Map<ConfigType, ConfigExtractor> configExtractorMap;

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

  public ChatLanguageModel getModel() {
    return model;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  private ChatLanguageModel createModel(ConfigExtractor configExtractor) {
    LLMType type = LLMType.valueOf(llmType);
    if (llmMap.containsKey(type)) {
      return llmMap.get(type).apply(configExtractor, this);
    }
    throw new IllegalArgumentException("Unsupported LLM type: " + llmType);
  }

  @Override
  public void initialise() throws InitialisationException {
    initRequiredConfigurations();
    ConfigExtractor configExtractor = configExtractorMap.get(ConfigType.fromValue(configType));
    model = createModel(configExtractor);
  }

  private void initRequiredConfigurations() {
    configExtractorMap = new HashMap<>();
    configExtractorMap.put(ConfigType.ENV_VARIABLE, new EnvConfigExtractor());
    configExtractorMap.put(ConfigType.CONFIG_JSON, new FileConfigExtractor(filePath, llmType));
  }
}
