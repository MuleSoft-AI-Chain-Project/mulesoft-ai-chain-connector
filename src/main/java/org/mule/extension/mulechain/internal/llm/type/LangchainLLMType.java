package org.mule.extension.mulechain.internal.llm.type;

import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.mistralai.MistralAiChatModelName;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiImageModelName;

import java.util.Arrays;
import java.util.stream.Stream;

public enum LangchainLLMType {
  OPENAI(getOpenAIModelNameStream()), GROQAI_OPENAI(OPENAI.getModelNameStream()), MISTRAL_AI(
      getMistralAIModelNameStream()), OLLAMA(
          getOllamaModelNameStream()), ANTHROPIC(getAnthropicModelNameStream()), AZURE_OPENAI(OPENAI.getModelNameStream());

  private final Stream<String> modelNameStream;

  LangchainLLMType(Stream<String> modelNameStream) {
    this.modelNameStream = modelNameStream;
  }

  public Stream<String> getModelNameStream() {
    return modelNameStream;
  }

  private static Stream<String> getOpenAIModelNameStream() {
    return Stream.concat(Arrays.stream(OpenAiChatModelName.values()), Arrays.stream(OpenAiImageModelName.values()))
        .map(String::valueOf);
  }

  private static Stream<String> getMistralAIModelNameStream() {
    return Arrays.stream(MistralAiChatModelName.values()).map(String::valueOf);
  }

  private static Stream<String> getOllamaModelNameStream() {
    return Arrays.stream(OllamaModelName.values()).map(String::valueOf);
  }

  private static Stream<String> getAnthropicModelNameStream() {
    return Arrays.stream(AnthropicChatModelName.values()).map(String::valueOf);
  }

  enum OllamaModelName {
    MISTRAL("mistral"), PHI3("phi3"), ORCA_MINI("orca-mini"), LLAMA2("llama2"), CODE_LLAMA("codellama"), TINY_LLAMA("tinyllama");

    private final String value;

    OllamaModelName(String value) {
      this.value = value;
    }

    public String toString() {
      return this.value;
    }
  }
}
