package org.mule.extension.langchain.internal.embedding.stores;

import static org.mapdb.Serializer.INTEGER;
import static org.mapdb.Serializer.STRING;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMParameters;
import org.mule.extension.langchain.internal.tools.DynamicToolWrapper;
import org.mule.extension.langchain.internal.tools.GenericRestApiTool;
import org.mule.extension.langchain.internal.tools.RestApiTool;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.HtmlTextExtractor;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;


import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.mapdb.Serializer;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainEmbeddingStoresOperations {


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
	        default:
	            throw new IllegalArgumentException("Unsupported LLM type: " + configuration.getLlmType());
	    }
	    return model;
	}

	

	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Load-document-txt-file")  
	  public String loadDocumentToStore(String data, String contextFile, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
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
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
	              .chatLanguageModel(model)
	              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
	              // .chatMemory() // you can override default chat memory
	              // .promptTemplate() // you can override default prompt template
	              .build();

	      String answer = chain.execute(data);
	      //System.out.println(answer); 
	      return answer;
	  }  
  


	  
	  
	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Load-document-pdf-file")  
	  public String loadDocumentPDFFile(String data, String contextFile, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              //.documentSplitter(DocumentSplitters.recursive(300, 0))
	              .documentSplitter(DocumentSplitters.recursive(100000, 200, new OpenAiTokenizer()))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
	      
	      Document document = loadDocument(contextFile, new ApacheTikaDocumentParser());
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
	              .chatLanguageModel(model)
	              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
	              // .chatMemory() // you can override default chat memory
	              // .promptTemplate() // you can override default prompt template
	              .build();

	      String answer = chain.execute(data);
	      //System.out.println(answer); 
	      return answer;
	  }  
  

	  
	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Load-document-from-url")  
	  public String loadDocumentFromURL(String data, String contextURL, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              //.documentSplitter(DocumentSplitters.recursive(300, 0))
	              .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
	      
	    URL url = null;
		try {
			url = new URL(contextURL);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	      Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
	      HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
	      Document document = transformer.transform(htmlDocument);
	      document.metadata().add("url", contextURL);
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
	              .chatLanguageModel(model)
	              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
	              // .chatMemory() // you can override default chat memory
	              // .promptTemplate() // you can override default prompt template
	              .build();

	      String answer = chain.execute(data);
	      //System.out.println(answer); 
	      return answer;
	  }  
  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  interface Assistant {

	      String chat(@MemoryId int memoryId, @UserMessage String userMessage);
	  }


	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Persistent-memory")  
	  public String chatWithPersistentMemory(String data, String dbFilePath, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	      
	      	ChatLanguageModel model = createModel(configuration, LangchainParams);
		  
	        //String dbFilePath = "/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db";
	        PersistentChatMemoryStore.initialize(dbFilePath);

			PersistentChatMemoryStore store = new PersistentChatMemoryStore();



	        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
	                .id(memoryId)
	                .maxMessages(10)
	                .chatMemoryStore(store)
	                .build();

	        Assistant assistant = AiServices.builder(Assistant.class)
	                .chatLanguageModel(model)
	                .chatMemoryProvider(chatMemoryProvider)
	                .build();

	        return assistant.chat(1, data);

	  }

	  static class PersistentChatMemoryStore implements ChatMemoryStore {

	      //private final DB db = DBMaker.fileDB("/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db").transactionEnable().fileLockDisable().make();
	      //private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();
	        private static DB db;
	        private static Map<Integer, String> map;
	        public static void initialize(String dbMFilePath) {
	            db = DBMaker.fileDB(dbMFilePath)
	                    .transactionEnable()
	                    .fileLockDisable()
	                    .make();
	            map = db.hashMap("messages", INTEGER, STRING).createOrOpen();
	        }

	      @Override
	      public List<ChatMessage> getMessages(Object memoryId) {
	          String json = map.get((int) memoryId);
	          return messagesFromJson(json);
	      }

	      @Override
	      public void updateMessages(Object memoryId, List<ChatMessage> messages) {
	          String json = messagesToJson(messages);
	          map.put((int) memoryId, json);
	          db.commit();
	      }

	      @Override
	      public void deleteMessages(Object memoryId) {
	          map.remove((int) memoryId);
	          db.commit();
	      }
	  }  
	  
//	  
//	  static class ToolConfigMemoryStore implements ChatMemoryStore {
//
//	      //private final DB db = DBMaker.fileDB("/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db").transactionEnable().fileLockDisable().make();
//	      //private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();
//	        private static DB db;
//	        private static Map<Integer, String> map;
//	        public static void initialize(String dbMFilePath, String collection) {
//	            db = DBMaker.fileDB(dbMFilePath)
//	                    .transactionEnable()
//	                    .fileLockDisable()
//	                    .make();
//	            map = db.hashMap(collection, Serializer.INTEGER, Serializer.STRING).createOrOpen();
//	        }
//
//	      @Override
//	      public List<ChatMessage> getMessages(Object memoryId) {
//	          String json = map.get((int) memoryId);
//	          return messagesFromJson(json);
//	      }
//
//	      @Override
//	      public void updateMessages(Object memoryId, List<ChatMessage> messages) {
//	          String json = messagesToJson(messages);
//	          map.put((int) memoryId, json);
//	          db.commit();
//	      }
//
//	      @Override
//	      public void deleteMessages(Object memoryId) {
//	          map.remove((int) memoryId);
//	          db.commit();
//	      }
//	  }  
//
//	  
//	  
//	  /**
//	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
//	   */
//	  @MediaType(value = ANY, strict = false)
//	  @Alias("Embedding-tools")  
//	  public String embeddingTools(String data, String memoryStore, String toolConfig, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
//	      
//	      	ChatLanguageModel model = createModel(configuration, LangchainParams);
//		  
//	        //String dbFilePath = "/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db";
//	      	ToolConfigMemoryStore.initialize(memoryStore, toolConfig);
//
//	      	ToolConfigMemoryStore store = new ToolConfigMemoryStore();
//
//	        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
//	                .id(memoryId)
//	                .maxMessages(100)
//	                .chatMemoryStore(store)
//	                .build();
//
//	        Assistant assistant = AiServices.builder(Assistant.class)
//	                .chatLanguageModel(model)
//	                .chatMemoryProvider(chatMemoryProvider)
//	                .build();
//
//	        return assistant.chat(1, data);
//
//	  }
//
//	  
//
//	  
//	  
//	  /**
//	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
//	   */
//	  @MediaType(value = ANY, strict = false)
//	  @Alias("Register-tools")  
//	  public boolean registerTools(String toolConfig, String memoryStore, String toolCollection) {
//	     
//	        
//	        // Initialize the ObjectMapper for JSON parsing
//	        ObjectMapper objectMapper = new ObjectMapper();
//	        
//	        try {
//	            // Read JSON file into a list of maps
//	            List<Map<String, Object>> jsonList = objectMapper.readValue(new File(toolConfig), new TypeReference<List<Map<String, Object>>>() {});
//	            
//	            // Open or create a MapDB database
//	            DB db = DBMaker.fileDB(memoryStore + ".db").make();
//	            
//	            // Create or open a map
//	            HTreeMap<Integer, String> map = db.hashMap(toolCollection, Serializer.INTEGER, Serializer.STRING).createOrOpen();
//	            
//	            // Store each JSON object in the map
//	            int index = 0;
//	            for (Map<String, Object> jsonObject : jsonList) {
//	                String jsonString = objectMapper.writeValueAsString(jsonObject);
//	                map.put(index++, jsonString);
//	                System.out.println(jsonString);
//	            }
//	            
//	            // Commit and close the database
//	            db.commit();
//	            db.close();
//	            
//	            System.out.println("JSON data has been stored in MapDB.");
//	            
//				return true;
//	            
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	            return false;
//	        }
//
//	  }
//
//	  /**
//	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
//	   */
//	  @MediaType(value = ANY, strict = false)
//	  @Alias("Read-tools")  
//	  public String readTools(String memoryStore, String toolCollection) {
//	     
//	        
//	        // Path to the MapDB database file
//	        String dbFilePath = memoryStore + ".db";
//
//	        // Initialize the ObjectMapper for JSON processing
//	        ObjectMapper objectMapper = new ObjectMapper();
//
//	        // Open the MapDB database
//	        DB db = DBMaker.fileDB(dbFilePath).make();
//
//	        // Create or open the map
//	        HTreeMap<Integer, String> map = db.hashMap(toolCollection, Serializer.INTEGER, Serializer.STRING).open();
//
//	        // List to store the JSON objects
//	        List<Map<String, Object>> jsonData = new ArrayList<>();
//
//	        // Iterate over the map entries and print the JSON data
//	        for (Map.Entry<Integer, String> entry : map.entrySet()) {
//	            try {
//	                // Convert the JSON string back to a Map
//	                Map<String, Object> jsonObject = objectMapper.readValue(entry.getValue(), Map.class);
//	                jsonData.add(jsonObject);
//	                // Print the JSON object (or process it as needed)
//	                System.out.println("Key: " + entry.getKey() + " - Value: " + jsonObject);
//
//	            } catch (IOException e) {
//	                e.printStackTrace();
//	            }
//	        }
//
//	        // Close the database
//	        db.close();
//			return jsonData.toString();
//	  }
//
	  
	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("Use-tools-dynamically")  
	  public String useTools(String data, String toolConfig, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              .documentSplitter(DocumentSplitters.recursive(30000, 200))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
	      
	      Document document = loadDocument(toolConfig, new TextDocumentParser());
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      
	      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
	              .chatLanguageModel(model)
	              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
	              // .chatMemory() // you can override default chat memory
	              // .promptTemplate() // you can override default prompt template
	              .build();

	      String intermediateAnswer = chain.execute(data);
	      //String name = chain.execute("What is the name from: " + intermediateAnswer + ". Reply only with the value.");
	      //String description = chain.execute("What is the description from: " + intermediateAnswer+ ". Reply only with the value.");
	      String apiEndpoint = chain.execute("What is the url from: " + intermediateAnswer+ ". Reply only with the value.");
	      System.out.println(intermediateAnswer); 
	      System.out.println(apiEndpoint); 

	      
	      // Create an instance of the custom tool with parameters
          GenericRestApiTool restApiTool = new GenericRestApiTool(apiEndpoint, "API Call", "Execute GET or POST Requests");
	      
	      
          ChatLanguageModel agent = OpenAiChatModel.builder()
                  .apiKey(configuration.getLlmApiKey())
                  .modelName(LangchainParams.getModelName())
                  .temperature(0.3)
                  .timeout(ofSeconds(60))
                  .logRequests(true)
                  .logResponses(true)
                  .build();
          // Build the assistant with the custom tool
          AssistantC assistant = AiServices.builder(AssistantC.class)
                  .chatLanguageModel(agent)
                  .tools(restApiTool)
                  .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
                  .build();
          // Use the assistant to make a query
          String response = assistant.chat(intermediateAnswer);
          System.out.println(response);
	      
	      
	      return response;
	  }  
  

		interface AssistantC {
			
		    String chat(String userMessage);
		}
  
		
		
		//************ IMPORTANT ******************//
		
		// TO DO TASKS SERIALIZATION AND DESERIALIZATION FOR STORE
        // In-memory embedding store can be serialized and deserialized to/from file
        // String filePath = "/home/me/embedding.store";
        // embeddingStore.serializeToFile(filePath);
        // InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(filePath);

  
}