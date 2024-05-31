package org.mule.extension.langchain.internal.tools;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.stream.Stream;

import org.mule.extension.langchain.internal.embedding.models.LangchainEmbeddingModelConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
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
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import dev.ai4j.openai4j.chat.AssistantMessage;
import dev.langchain4j.agent.tool.*;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.langchain4j.classification.EmbeddingModelTextClassifier;
import dev.langchain4j.classification.*;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import static embedding.classification.EmbeddingModelTextClassifierExample.CustomerServiceCategory.*;
import static java.util.Arrays.asList;


import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainToolsOperations {

	
	
	

  /**
   * Example of an operation that uses the configuration and a connection instance to perform some action.
   */
//  @MediaType(value = ANY, strict = false)
//  @Alias("Use-static-tools")
//  public String predict(String prompt, String endpointUrl, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams){
//	  
// 
//      // Create an instance of the custom tool with parameters
//      RestApiTool restApiTool = new RestApiTool(
//    		  endpointUrl, 
//              "Get Inventory", 
//              "Get inventory from SAP ERP system for Material MULETEST0"
//      );
//      
//      
//      //https://docs.langchain4j.dev/tutorials/tools/
//
//      ChatLanguageModel model = OpenAiChatModel.builder()
//              .apiKey(configuration.getLlmApiKey())
//              .modelName(LangchainParams.getModelName())
//              .temperature(0.3)
//              .timeout(ofSeconds(60))
//              .logRequests(true)
//              .logResponses(true)
//              .build();
//      // Build the assistant with the custom tool
//      Assistant assistant = AiServices.builder(Assistant.class)
//              .chatLanguageModel(model)
//              .tools(restApiTool)
//              .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//              .build();
//      // Use the assistant to make a query
//      String response = assistant.chat(prompt);
//      //System.out.println(response);
//
//	  return response;
//  }
//
//	interface Assistant {
//	
//	    String chat(String userMessage);
//	}
//	
	
	
//	/**
//	 * Example of an operation that uses the configuration and a connection instance to perform some action.
//	 */
//	@MediaType(value = ANY, strict = false)
//	@Alias("Register-tools")
//	public String registerTools(String fileName, String endpointUrl, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams){
//		
//		
//		
//		return "true";
//	
//	}
//	
	
	
//    @MediaType(value = ANY, strict = false)
//    @Alias("Dynamic-tools")
//    public String executeDynamically(String prompt, String jsonFilePath, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
//        List<Tool> tools = new ArrayList<>();
//
//        try {
//            // Read the JSON file
//            String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
//            JSONArray jsonArray = new JSONArray(content);
//
//            // Iterate through the JSON array and create RestApiTool instances
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                String apiEndpoint = jsonObject.getString("url");
//                String name = jsonObject.getString("name");
//                String description = jsonObject.getString("description");
//
//                // Create an instance of RestApiTool
//                GenericRestApiTool restApiTool = new GenericRestApiTool(apiEndpoint, name, description);
//
//                // Create dynamic Tool annotation
//                Tool dynamicTool = DynamicToolWrapper.create(name, description);
//
//                // Use reflection to assign the dynamic Tool annotation to the execute method
//                Method executeMethod = GenericRestApiTool.class.getMethod("execute", String.class);
//                if (executeMethod.isAnnotationPresent(Tool.class)) {
//                    // You need to add restApiTool as a Tool to the tools list
//                    tools.add((Tool) restApiTool);
//                    // Add the dynamic tool to the list
//                    tools.add(dynamicTool);
//                }
//            }
//        } catch (IOException | NoSuchMethodException e) {
//            e.printStackTrace();
//            return "Error reading JSON file: " + e.getMessage();
//        }
//
//        // Create the ChatLanguageModel and Assistant
//        ChatLanguageModel model = OpenAiChatModel.builder()
//                .apiKey(configuration.getLlmApiKey())
//                .modelName(LangchainParams.getModelName())
//                .temperature(0.3)
//                .timeout(ofSeconds(60))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//
//        Assistant assistant = AiServices.builder(Assistant.class)
//                .chatLanguageModel(model)
//                .tools(tools)
//                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
//                .build();
//
//        // Use the assistant to make a query
//        String response = assistant.chat(prompt);
//
//        return response;
//    }	
//	
    
    
    
    
    
    
}