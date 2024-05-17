package org.mule.extension.langchain.internal.llm;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;


import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import java.util.HashMap;
import java.util.Map;
import static java.time.Duration.ofSeconds;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;


import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;



/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {

	
	

  /**
   * Example of an operation that uses the configuration and a connection instance to perform some action.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Answer-prompt")
  public String predict(String prompt, @Config LangchainLLMConfiguration configuration){
	    
	  ChatLanguageModel model = OpenAiChatModel.withApiKey(configuration.getLlmApiKey());

	  String answer = model.generate(prompt);

	    
	  //return "Using Configuration [" + configuration.getLlmApiKey() + "] with Connection id [" + connection.getId() + "] " + "] with Prompt [" + prompt + "] ";
	  return answer;
  }

  
  
  
  
  
  /**
   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Answer-prompt-by-model")  
  public String answerPromptByModelName(String prompt, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
      // OpenAI parameters are explained here: https://platform.openai.com/docs/api-reference/chat/create

      ChatLanguageModel model = OpenAiChatModel.builder()
              .apiKey(configuration.getLlmApiKey())
              .modelName(LangchainParams.getModelName())
              .temperature(0.3)
              .timeout(ofSeconds(60))
              .logRequests(true)
              .logResponses(true)
              .build();

      String answer = prompt;

      String response = model.generate(answer);

      //System.out.println(response);
	return response;
 }
  
  
    

  
  
  /**
   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Define-prompt-template")  
  public String definePromptTemplate(String template, String instructions, String dataset, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {

      ChatLanguageModel model = OpenAiChatModel.builder()
              .apiKey(configuration.getLlmApiKey())
              .modelName(LangchainParams.getModelName())
              .temperature(0.3)
              .timeout(ofSeconds(60))
              .logRequests(true)
              .logResponses(true)
              .build();

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
   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Sentiment-Analyzer")  
  public Sentiment extractSentiments(String data, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
  
      ChatLanguageModel model = OpenAiChatModel.builder()
              .apiKey(configuration.getLlmApiKey())
              .modelName(LangchainParams.getModelName())
              .temperature(0.3)
              .timeout(ofSeconds(60))
              .logRequests(true)
              .logResponses(true)
              .build();


      SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

      Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf(data);
      System.out.println(sentiment); // POSITIVE

      boolean positive = sentimentAnalyzer.isPositive(data);
      System.out.println(positive); // false
      
      return sentiment;
  }

  
  
  /**
   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Run-with-memory")  
  public String runWithMemory(String data, String contextFile, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
  
      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
              .documentSplitter(DocumentSplitters.recursive(300, 0))
              .embeddingModel(embeddingModel)
              .embeddingStore(embeddingStore)
              .build();

      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
      
      Document document = loadDocument(contextFile, new TextDocumentParser());
      ingestor.ingest(document);

      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
              .chatLanguageModel(OpenAiChatModel.builder()
                      .apiKey(configuration.getLlmApiKey())
                      .modelName(LangchainParams.getModelName())
                      .temperature(0.3)
                      .timeout(ofSeconds(60))
                      .logRequests(true)
                      .logResponses(true)
                      .build())
              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
              // .chatMemory() // you can override default chat memory
              // .promptTemplate() // you can override default prompt template
              .build();

      String answer = chain.execute(data);
      //System.out.println(answer); 
      return answer;
  }
  
  
  private static Path toPath(String fileName) {
      try {
          URL fileUrl = LangchainLLMOperations.class.getResource(fileName);
          return Paths.get(fileUrl.toURI());
      } catch (URISyntaxException e) {
          throw new RuntimeException(e);
      }
  }  
  
  
  
}

  
  /*   
   * https://docs.mulesoft.com/mule-sdk/latest/define-operations
   * Define output resolver
   * 
  interface Assistant {

      TokenStream chat(String message);
  }
  
  @MediaType(value = ANY, strict = false)
  @Alias("Stream-prompt-answer")  
  public TokenStream streamingPrompt(String prompt, @Config LangchaintemplateConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchaintemplateParameters LangchainParams) {

      // Sorry, "demo" API key does not support streaming (yet). Please use your own key.
      StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
              .apiKey(configuration.getLlmApiKey())
              .modelName(LangchainParams.getModelName())
              .temperature(0.3)
              .timeout(ofSeconds(60))
              .logRequests(true)
              .logResponses(true)
              .build();


      Assistant assistant = AiServices.create(Assistant.class, model);

      TokenStream tokenStream = assistant.chat(prompt);

      tokenStream.onNext(System.out::println)
              .onComplete(System.out::println)
              .onError(Throwable::printStackTrace)
              .start();
      
	  return tokenStream;
 
  } */

