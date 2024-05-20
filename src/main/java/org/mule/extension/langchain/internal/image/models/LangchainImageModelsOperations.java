package org.mule.extension.langchain.internal.image.models;

import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import static java.time.Duration.ofSeconds;


/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainImageModelsOperations {


	private ChatLanguageModel createModel(LangchainLLMConfiguration configuration, LangchainLLMParameters LangchainParams) {
	    ChatLanguageModel model = null;
	    switch (configuration.getLlmType()) {
	        case "OPENAI_API_KEY":
	  	      //System.out.println(configuration.getLlmType() + " - " + configuration.getLlmApiKey()); 
	            model = OpenAiChatModel.builder()
	                    .apiKey(configuration.getLlmApiKey()) 
	                    .modelName(LangchainParams.getModelName())
	                    .maxTokens(500)
	                    .build();
	            break;
	        case "MISTRALAI_API_KEY":
	            model = MistralAiChatModel.builder()
	                    .apiKey(configuration.getLlmApiKey())
	                    .modelName(LangchainParams.getModelName())
	                    .temperature(0.3)
	                    .timeout(ofSeconds(60))
	                    .logRequests(true)
	                    .logResponses(true)
	                    .build();
	            break;
	        case "OLLAMA_BASE_URL":
	            model = OllamaChatModel.builder()
	                    .baseUrl(configuration.getLlmApiKey())
	                    .modelName(LangchainParams.getModelName())
	                    .build();
	            break;
	        case "ANTHROPIC_API_KEY":
	            model = AnthropicChatModel.builder()
	                    .apiKey(configuration.getLlmApiKey())
	                    .modelName(LangchainParams.getModelName())
	                    .logRequests(true)
	                    .logResponses(true)
	                    .build();
	            break;
	        default:
	            throw new IllegalArgumentException("Unsupported LLM type: " + configuration.getLlmType());
	    }
	    return model;
	}

	

	
	

	
	

	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Read-from-image")  
	  public String readFromImage(String data, String contextURL, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      ChatLanguageModel model = createModel(configuration, LangchainParams);

          UserMessage userMessage = UserMessage.from(
                  TextContent.from(data),
                  ImageContent.from(contextURL)
          );

          Response<AiMessage> response = model.generate(userMessage);

          return response.content().text();
	  }  
  

	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Generate-image")  
	  public URI drawImage(String data, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
          ImageModel model = OpenAiImageModel.builder()
                  .modelName(LangchainParams.getModelName())
                  .apiKey(configuration.getLlmApiKey())
                  .build();

          Response<Image> response = model.generate(data);
          System.out.println(response.content().url());
          return response.content().url();
	  }  
  

	  
	  
}