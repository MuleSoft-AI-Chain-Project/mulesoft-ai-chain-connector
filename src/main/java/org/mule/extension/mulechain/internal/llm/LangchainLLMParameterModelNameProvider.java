package org.mule.extension.mulechain.internal.llm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class LangchainLLMParameterModelNameProvider implements ValueProvider {

  private static final Map<LLMType, Stream<String>> valueMap;

  static {
    valueMap = new HashMap<>();
    Stream<String> openAiStream = Stream
        .concat(Arrays.stream(OpenAiChatModelName.values()), Arrays.stream(OpenAiImageModelName.values())).map(String::valueOf);
    valueMap.put(LLMType.OPENAI, openAiStream);
    valueMap.put(LLMType.GROQAI_OPENAI, openAiStream);
    valueMap.put(LLMType.MISTRAL_AI, Arrays.stream(MistralAiChatModelName.values()).map(String::valueOf));
    valueMap.put(LLMType.OLLAMA,
                 Arrays.stream(new String[] {"mistral", "phi3", "orca-mini", "llama2", "codellama", "tinyllama"}));
    valueMap.put(LLMType.ANTHROPIC, Arrays.stream(AnthropicChatModelName.values()).map(String::valueOf));
    valueMap.put(LLMType.AZURE_OPENAI, openAiStream);
  }

  @Parameter
  private String llmType;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return ValueBuilder.getValuesFor(valueMap.get(LLMType.valueOf(llmType)));
  }

}
