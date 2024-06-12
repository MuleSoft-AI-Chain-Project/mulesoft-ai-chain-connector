package org.mule.extension.langchain.internal.embedding.stores;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import static org.mapdb.Serializer.STRING;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMParameters;
import org.mule.extension.langchain.internal.helpers.fileTypeParameters;
import org.mule.extension.langchain.internal.tools.GenericRestApiTool;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import org.mule.runtime.extension.api.annotation.param.Config;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.transformer.HtmlTextExtractor;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import software.amazon.awssdk.regions.Region;

import java.net.MalformedURLException;
import java.net.URL;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;


import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	

/* 	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
/* 		  @MediaType(value = ANY, strict = false)
	  @Alias("Load-document-txt-file")  
	  public String loadDocumentText(String data, String contextFile, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
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
	      
	      
	      // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
	      // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
	      //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services
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
   */


	  
	  
	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
/* 		  @MediaType(value = ANY, strict = false)
	  @Alias("Load-document-pdf-file")  
	  public String loadDocumentPDFFile(String data, String contextFile, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              //.documentSplitter(DocumentSplitters.recursive(300, 0))
	              .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
	      
	      Document document = loadDocument(contextFile, new ApacheTikaDocumentParser());
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
	      // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
	      //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services
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
 */  

	  
	  /**
	   * Example of a simple operation that receives a string parameter and returns a new string message that will be set on the payload.
	   */
/* 		  @MediaType(value = ANY, strict = false)
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
	      
	      

	      // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
	      // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
	      //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services
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
   */
	  
	  
	  
	  
	  
	  /**
	   * Helps perform semantic search on documents of type text. This includes, text files (txt, json, xml, etc.), pdf files (not scanned), and websites via url.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("RAG-load-document")  
	  public String loadDocumentFile(String data, String contextPath, @ParameterGroup(name="Context") fileTypeParameters fileType, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

		  System.out.println(fileType.getFileType());
		  
		 // ChatLanguageModel model = null;
		 Document document = null;
		  switch (fileType.getFileType()) {
			case "text":
				document = loadDocument(contextPath, new TextDocumentParser());
				ingestor.ingest(document);
				break;
			case "pdf":
				document = loadDocument(contextPath, new ApacheTikaDocumentParser());
				ingestor.ingest(document);
				  break;
			case "url":
				URL url = null;
				try {
					url = new URL(contextPath);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
				HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
				document = transformer.transform(htmlDocument);
				document.metadata().add("url", contextPath);
				ingestor.ingest(document);
				break;
			default:
				throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
		  }
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      
	      // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
	      // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
	      //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services

		  ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);
		      
		  AssistantEmbedding assistant = AiServices.builder(AssistantEmbedding.class)
					.chatLanguageModel(model)
					.contentRetriever(contentRetriever)
					.build();

	      String answer = assistant.chat(data);
	      //System.out.println(answer); 
	      return answer;
	  }  
  
	  








	  
	  
	  
	  
	interface Assistant {

		String chat(@MemoryId int memoryId, @UserMessage String userMessage);
	}

	interface AssistantMemory {

		String chat(@MemoryId String memoryName, @UserMessage String userMessage);
	}





	  /**
	   * Implements a chat memory for a defined LLM as an AI Agent. The memoryName is allows the multi-channel / profile design.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("CHAT-answer-prompt-with-memory")  
	  public String chatWithPersistentMemory(String data, String memoryName, String dbFilePath, int maxMessages, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	      
	      	ChatLanguageModel model = createModel(configuration, LangchainParams);
		  
	        //String dbFilePath = "/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db";
	        PersistentChatMemoryStore.initialize(dbFilePath);

			PersistentChatMemoryStore store = new PersistentChatMemoryStore();



			ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
	                .id(memoryName)
	                .maxMessages(maxMessages)
	                .chatMemoryStore(store)
	                .build();

			AssistantMemory assistant = AiServices.builder(AssistantMemory.class)
	                .chatLanguageModel(model)
	                .chatMemoryProvider(chatMemoryProvider)
	                .build();

			return assistant.chat(memoryName, data);

	  }

	  static class PersistentChatMemoryStore implements ChatMemoryStore {

	      //private final DB db = DBMaker.fileDB("/Users/amir.khan/Documents/langchain4mule resources/multi-user-chat-memory.db").transactionEnable().fileLockDisable().make();
	      //private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();
	        private static DB db;
	      //  private static Map<Integer, String> map;
	        private static Map<String, String> map;
	        public static void initialize(String dbMFilePath) {
	            db = DBMaker.fileDB(dbMFilePath)
	                    .transactionEnable()
	                    .fileLockDisable()
	                    .make();
				//map = db.hashMap("messages", INTEGER, STRING).createOrOpen();
				map = db.hashMap("messages", STRING, STRING).createOrOpen();
					}

	      @Override
	      public List<ChatMessage> getMessages(Object memoryId) {
	          String json = map.get((String) memoryId);
	          return messagesFromJson(json);
	      }

	      @Override
	      public void updateMessages(Object memoryId, List<ChatMessage> messages) {
	          String json = messagesToJson(messages);
	          map.put((String) memoryId, json);
	          db.commit();
	      }

	      @Override
	      public void deleteMessages(Object memoryId) {
	          map.remove((String) memoryId);
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
	   * (Legacy) Usage of tools by a defined AI Agent. Provide a list of tools (APIs) with all required informations (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("TOOLS-use-ai-service-legacy")  
	  public String useTools(String data, String toolConfig, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              .documentSplitter(DocumentSplitters.recursive(30000, 200))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      
	      Document document = loadDocument(toolConfig, new TextDocumentParser());
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      
	      // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
	      // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
	      //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services
	      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
	              .chatLanguageModel(model)
	              .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
	              // .chatMemory() // you can override default chat memory
	              // .promptTemplate() // you can override default prompt template
	              .build();

	      String intermediateAnswer = chain.execute(data);
	      String response;
	      List<String> findURL = extractUrls(intermediateAnswer);
	      if (findURL!=null){
	    	  
		      //String name = chain.execute("What is the name from: " + intermediateAnswer + ". Reply only with the value.");
		      //String description = chain.execute("What is the description from: " + intermediateAnswer+ ". Reply only with the value.");
		      String apiEndpoint = chain.execute("What is the url from: " + intermediateAnswer+ ". Reply only with the value.");
		      System.out.println("intermediate Answer: " + intermediateAnswer); 
		      System.out.println("apiEndpoint: " + apiEndpoint); 
	
		      
		      // Create an instance of the custom tool with parameters
	          GenericRestApiTool restApiTool = new GenericRestApiTool(apiEndpoint, "API Call", "Execute GET or POST Requests");
		      
		      
	          ChatLanguageModel agent = OpenAiChatModel.builder()
	                  .apiKey(configuration.getLlmApiKey())
	                  .modelName(LangchainParams.getModelName())
	                  .temperature(0.1)
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
	           response = assistant.chat(intermediateAnswer);
	          System.out.println(response);
	      } else{
	    	  response =  intermediateAnswer;
	      }
	      
	      
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

		
	    private static List<String> extractUrls(String input) {
	        // Define the URL pattern
	        String urlPattern = "(https?://\\S+\\b)";
	        
	        // Compile the pattern
	        Pattern pattern = Pattern.compile(urlPattern);
	        
	        // Create a matcher from the input string
	        Matcher matcher = pattern.matcher(input);
	        
	        // Find and collect all matches
	        List<String> urls = new ArrayList<>();
	        while (matcher.find()) {
	            urls.add(matcher.group());
	        }
	        
	        // Return null if no URLs are found
	        return urls.isEmpty() ? null : urls;
	    }
		
  
	    
	    
	    
	    
	    ////////////////////////////////////////////
	    
	    
	    
		  /**
		   * Create a new embedding store (in-memory), which is exported to the defined storeName (full path)
		   */
		  @MediaType(value = ANY, strict = false)
		  @Alias("EMBEDDING-new-store")  
		  public String createEmbedding(String storeName) {
			
			InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
						
			//embeddingStore.serializeToFile(storeName);
			embeddingStore.serializeToFile(storeName);
	    
			return "Embedding-store created.";
		  }
	    
	    
	    
		  /**
		   * Add document of type text, pdf and url to embedding store (in-memory), which is exported to the defined storeName (full path)
		   */
		  @MediaType(value = ANY, strict = false)
		  @Alias("EMBEDDING-add-document-to-store")  
		  public String addFileEmbedding(String storeName, String contextPath, @ParameterGroup(name="Context") fileTypeParameters fileType, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {

			  EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

		      InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
		      //EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

		      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
		              .documentSplitter(DocumentSplitters.recursive(300, 0))
		              .embeddingModel(embeddingModel)
		              .embeddingStore(deserializedStore)
		              .build();

		      //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());
		      
		      //Document document = loadDocument(contextFile, new TextDocumentParser());
		      //ingestor.ingest(document);

			  

		 // ChatLanguageModel model = null;
		 Document document = null;
		  switch (fileType.getFileType()) {
			case "text":
				document = loadDocument(contextPath, new TextDocumentParser());
				ingestor.ingest(document);
				break;
			case "pdf":
				document = loadDocument(contextPath, new ApacheTikaDocumentParser());
				ingestor.ingest(document);
				  break;
			case "url":
				URL url = null;
				try {
					url = new URL(contextPath);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				
				Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
				HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
				document = transformer.transform(htmlDocument);
				document.metadata().add("url", contextPath);
				ingestor.ingest(document);
				break;
			default:
				throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
		  }






		      deserializedStore.serializeToFile(storeName);
			return "Embedding-store updated.";
		  }
	    
	    
		  /**
		   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
		   */
		  @MediaType(value = ANY, strict = false)
		  @Alias("EMBEDDING-get-info-from-store")  
		  public String promptFromEmbedding(String storeName, String data, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
			  EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

		      InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
		      //EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		      
		      ChatLanguageModel model = createModel(configuration, LangchainParams);
		      
		      
		      ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(deserializedStore, embeddingModel);
		      
		      AssistantEmbedding assistant = AiServices.builder(AssistantEmbedding.class)
		    		    .chatLanguageModel(model)
		    		    .contentRetriever(contentRetriever)
		    		    .build();

//		      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
//		              .chatLanguageModel(model)
//		              .retriever(EmbeddingStoreRetriever.from(deserializedStore, embeddingModel))
//		              // .chatMemory() // you can override default chat memory
//		              // .promptTemplate() // you can override default prompt template
//		              .build();
//
//		      String answer = chain.execute(data);
		      String response = assistant.chat(data);
		      //System.out.println(answer); 

		      deserializedStore.serializeToFile(storeName);

			return response;
		  }
	    
	    
	    
		  interface AssistantEmbedding {

			    String chat(String userMessage);
			}
   

	   /**
	   * (AI Services) Usage of tools by a defined AI Agent. Provide a list of tools (APIs) with all required informations (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
	   */
	  @MediaType(value = ANY, strict = false)
	  @Alias("TOOLS-use-ai-service")  
	  public String useAIServiceTools(String data, String toolConfig, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
	  
	      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

	      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

	      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
	              .documentSplitter(DocumentSplitters.recursive(30000, 200))
	              .embeddingModel(embeddingModel)
	              .embeddingStore(embeddingStore)
	              .build();

	      
	      Document document = loadDocument(toolConfig, new TextDocumentParser());
	      ingestor.ingest(document);
	      
	      
	      ChatLanguageModel model = createModel(configuration, LangchainParams);
	      
	      

	      
		  ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);
		      
		  AssistantEmbedding assistant = AiServices.builder(AssistantEmbedding.class)
					.chatLanguageModel(model)
					.contentRetriever(contentRetriever)
					.build();


		  String intermediateAnswer = assistant.chat(data);
		  String response;
	      List<String> findURL = extractUrls(intermediateAnswer);
	      if (findURL!=null){
	    	  
		      //String name = chain.execute("What is the name from: " + intermediateAnswer + ". Reply only with the value.");
		      //String description = chain.execute("What is the description from: " + intermediateAnswer+ ". Reply only with the value.");
		      String apiEndpoint = assistant.chat("What is the url from: " + intermediateAnswer+ ". Reply only with the value.");
		      System.out.println("intermediate Answer: " + intermediateAnswer); 
		      System.out.println("apiEndpoint: " + apiEndpoint); 
	
		      
		      // Create an instance of the custom tool with parameters
	          GenericRestApiTool restApiTool = new GenericRestApiTool(apiEndpoint, "API Call", "Execute GET or POST Requests");
		      
		      
	          ChatLanguageModel agent = OpenAiChatModel.builder()
	                  .apiKey(configuration.getLlmApiKey())
	                  .modelName(LangchainParams.getModelName())
	                  .temperature(0.1)
	                  .timeout(ofSeconds(60))
	                  .logRequests(true)
	                  .logResponses(true)
	                  .build();
	          // Build the assistant with the custom tool
	          AssistantC assistantC = AiServices.builder(AssistantC.class)
	                  .chatLanguageModel(agent)
	                  .tools(restApiTool)
	                  .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
	                  .build();
	          // Use the assistant to make a query
	           response = assistantC.chat(intermediateAnswer);
	          System.out.println(response);
	      } else{
	    	  response =  intermediateAnswer;
	      }
	      
	      
		return response;
	  }  

			

	    
}