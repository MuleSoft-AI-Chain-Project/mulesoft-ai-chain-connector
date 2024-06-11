package org.mule.extension.langchain.internal.llm;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;


import static java.time.Duration.ofSeconds;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import software.amazon.awssdk.regions.Region;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static dev.langchain4j.internal.Utils.readBytes;
import static dev.langchain4j.model.bedrock.BedrockMistralAiChatModel.Types.Mistral7bInstructV0_2;
import static dev.langchain4j.model.bedrock.BedrockMistralAiChatModel.Types.MistralMixtral8x7bInstructV0_1;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static org.mapdb.Serializer.INTEGER;
import static org.mapdb.Serializer.STRING;
/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {


	private ChatLanguageModel createModel(LangchainLLMConfiguration configuration, LangchainLLMParameters LangchainParams) {
	    ChatLanguageModel model = null;
	    switch (configuration.getLlmType()) {
	        case "OPENAI_API_KEY":
	            model = OpenAiChatModel.builder()
	                    .apiKey(configuration.getLlmApiKey())
	                    .modelName(LangchainParams.getModelName())
	                    .temperature(0.3)
	                    .timeout(ofSeconds(60))
	                    .logRequests(true)
	                    .logResponses(true)
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
			case "AWS_BEDROCK_ID_AND_SECRET":
				//String[] creds = configuration.getLlmApiKey().split("mulechain"); 
				// For authentication, set the following environment variables:
        		// AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 				model = BedrockAnthropicMessageChatModel.builder()
						.region(Region.US_EAST_1)
						.temperature(0.30f)
						.maxTokens(300)
						.model(LangchainParams.getModelName())
						.maxRetries(1)
						.build();
	            break;
	        default:
	            throw new IllegalArgumentException("Unsupported LLM type: " + configuration.getLlmType());
	    }
	    return model;
	}

  
  /**
   * Implements a simple Chat agent
   */
  @MediaType(value = ANY, strict = false)
  @Alias("CHAT-answer-prompt")  
  public String answerPromptByModelName(String prompt, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
      // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create

	  
	    ChatLanguageModel model = createModel(configuration, LangchainParams);

	    String answer = prompt;
	    String response = model.generate(answer);

	    // System.out.println(response);
	    return response;

 }
  
  
    

  
  
  /**
   * Helps defining an AI Agent with a prompt template
   */
  @MediaType(value = ANY, strict = false)
  @Alias("AGENT-define-prompt-template")  
  public String definePromptTemplate(String template, String instructions, String dataset, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {

	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	  

          String templateString = template;
          PromptTemplate promptTemplate = PromptTemplate.from(templateString + System.lineSeparator() + "Instructions: {{instructions}}" + System.lineSeparator() + "Dataset: {{dataset}}");

          Map<String, Object> variables = new HashMap<>();
          variables.put("instructions", instructions);
          variables.put("dataset", dataset);

          Prompt prompt = promptTemplate.apply(variables);

          String response = model.generate(prompt.text());

          //System.out.println(response);
      	return response;
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
  public Sentiment extractSentiments(String data, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
  
      ChatLanguageModel model = createModel(configuration, LangchainParams);
	  

      SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

      Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf(data);
      System.out.println(sentiment); // POSITIVE

      boolean positive = sentimentAnalyzer.isPositive(data);
      System.out.println(positive); // false
      
      return sentiment;
  }

  
  
}
