package org.mule.extension.langchain.internal.streaming;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.stream.Stream;
import java.util.List;

import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.Streaming;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
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
public class LangchainLLMStreamingOperations {

	
	

  
  /*   
   * https://docs.mulesoft.com/mule-sdk/latest/define-operations
   * Define output resolver
   *  */
  interface Assistant {

      TokenStream chat(String message);
  }
  
  @MediaType(value = ANY, strict = false)
  @Alias("Stream-prompt-answer")  
  @OutputResolver(output = TokenStreamOutputResolver.class)
  @Streaming
  public TokenStream streamingPrompt(String prompt, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {

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

	  
  }
}
