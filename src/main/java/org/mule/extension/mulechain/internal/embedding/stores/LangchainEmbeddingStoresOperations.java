package org.mule.extension.mulechain.internal.embedding.stores;

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
import org.mule.extension.mulechain.internal.helpers.fileTypeParameters;
import org.mule.extension.mulechain.internal.llm.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.llm.LangchainLLMParameters;
import org.mule.extension.mulechain.internal.tools.GenericRestApiTool;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import org.mule.runtime.extension.api.annotation.param.Config;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
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
import java.net.MalformedURLException;
import java.net.URL;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;


import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainEmbeddingStoresOperations {


	private static JSONObject readConfigFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            try {
                String content = new String(Files.readAllBytes(path));
                return new JSONObject(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist: " + filePath);
        }
        return null;
    }

	private static OpenAiChatModel createOpenAiChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(LangchainParams.getModelName())
                .temperature(0.1)
                .timeout(ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

    }

	private static MistralAiChatModel createMistralAiChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
        return MistralAiChatModel.builder()
				//.apiKey(configuration.getLlmApiKey())
				.apiKey(apiKey)
				.modelName(LangchainParams.getModelName())
				.temperature(0.1)
				.timeout(ofSeconds(60))
				.logRequests(true)
				.logResponses(true)
				.build();
    }

	private static OllamaChatModel createOllamaChatModel(String baseURL, LangchainLLMParameters LangchainParams) {
        return OllamaChatModel.builder()
				//.baseUrl(configuration.getLlmApiKey())
				.baseUrl(baseURL)
				.modelName(LangchainParams.getModelName())
				.build();
    }


	private static AnthropicChatModel createAnthropicChatModel(String apiKey, LangchainLLMParameters LangchainParams) {
        return AnthropicChatModel.builder()
				//.apiKey(configuration.getLlmApiKey())
				.apiKey(apiKey)
				.modelName(LangchainParams.getModelName())
				.logRequests(true)
				.logResponses(true)
				.build();
    }


	private static AzureOpenAiChatModel createAzureOpenAiChatModel(String apiKey, String llmEndpoint, String deploymentName, LangchainLLMParameters LangchainParams) {
        return AzureOpenAiChatModel.builder()
				.apiKey(apiKey)
				.endpoint(llmEndpoint)
				.deploymentName(deploymentName)
				.temperature(0.1)
				.logRequestsAndResponses(true)
				.build();
	}



	private ChatLanguageModel createModel(LangchainLLMConfiguration configuration, LangchainLLMParameters LangchainParams) {
	    ChatLanguageModel model = null;
        JSONObject config = readConfigFile(configuration.getFilePath());

	    switch (configuration.getLlmType()) {
	        case "OPENAI":
				if (configuration.getConfigType() .equals("Environment Variables")) {
					model = createOpenAiChatModel(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
				} else {
					JSONObject llmType = config.getJSONObject("OPENAI");
					String llmTypeKey = llmType.getString("OPENAI_API_KEY");
					model = createOpenAiChatModel(llmTypeKey, LangchainParams);

				}
	            break;
	        case "MISTRAL_AI":
				if (configuration.getConfigType() .equals("Environment Variables")) {
					model = createMistralAiChatModel(System.getenv("MISTRAL_AI_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
				} else {
					JSONObject llmType = config.getJSONObject("MISTRAL_AI");
					String llmTypeKey = llmType.getString("MISTRAL_AI_API_KEY");
					model = createMistralAiChatModel(llmTypeKey, LangchainParams);

				}
	            break;
	        case "OLLAMA":
				if (configuration.getConfigType() .equals("Environment Variables")) {
					model = createOllamaChatModel(System.getenv("OLLAMA_BASE_URL").replace("\n", "").replace("\r", ""), LangchainParams);
				} else {
					JSONObject llmType = config.getJSONObject("OLLAMA");
					String llmTypeUrl = llmType.getString("OLLAMA_BASE_URL");
					model = createOllamaChatModel(llmTypeUrl, LangchainParams);

				}
	            break;
	        case "ANTHROPIC":
				if (configuration.getConfigType() .equals("Environment Variables")) {
					model = createAnthropicChatModel(System.getenv("ANTHROPIC_API_KEY").replace("\n", "").replace("\r", ""), LangchainParams);
				} else {
					JSONObject llmType = config.getJSONObject("ANTHROPIC");
					String llmTypeKey = llmType.getString("ANTHROPIC_API_KEY");
					model = createAnthropicChatModel(llmTypeKey, LangchainParams);
				}
	            break;
/* 			case "AWS_BEDROCK":
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
 */			case "AZURE_OPENAI":
 				if (configuration.getConfigType() .equals("Environment Variables")) {
					model = createAzureOpenAiChatModel(System.getenv("AZURE_OPENAI_KEY").replace("\n", "").replace("\r", ""), 
														System.getenv("AZURE_OPENAI_ENDPOINT").replace("\n", "").replace("\r", ""), 
														System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME").replace("\n", "").replace("\r", ""), LangchainParams);
				} else {
					JSONObject llmType = config.getJSONObject("AZURE_OPENAI");
					String llmTypeKey = llmType.getString("AZURE_OPENAI_KEY");
					String llmEndpoint = llmType.getString("AZURE_OPENAI_ENDPOINT");
					String llmDeploymentName = llmType.getString("AZURE_OPENAI_DEPLOYMENT_NAME");
					model = createAzureOpenAiChatModel(llmTypeKey, llmEndpoint, llmDeploymentName, LangchainParams);
				}
				break;
	        default:
	            throw new IllegalArgumentException("Unsupported LLM type: " + configuration.getLlmType());
	    }
	    return model;
	}

	


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
	      String response = model.generate(data);
	      List<String> findURL = extractUrls(intermediateAnswer);
	      if (findURL!=null){
	    	  
		      //String name = chain.execute("What is the name from: " + intermediateAnswer + ". Reply only with the value.");
		      //String description = chain.execute("What is the description from: " + intermediateAnswer+ ". Reply only with the value.");
		      //String apiEndpoint = chain.execute("What is the url from: " + intermediateAnswer+ ". Reply only with the value.");
		      //System.out.println("intermediate Answer: " + intermediateAnswer); 
		      //System.out.println("apiEndpoint: " + apiEndpoint); 
	
		      
		      // Create an instance of the custom tool with parameters
	          GenericRestApiTool restApiTool = new GenericRestApiTool(findURL.get(0), "API Call", "Execute GET or POST Requests");
		      
		      ChatLanguageModel agent = createModel(configuration, LangchainParams);
	        //   ChatLanguageModel agent = OpenAiChatModel.builder()
			//   		  .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
	        //           .modelName(LangchainParams.getModelName())
	        //           .temperature(0.1)
	        //           .timeout(ofSeconds(60))
	        //           .logRequests(true)
	        //           .logResponses(true)
	        //           .build();
	          // Build the assistant with the custom tool
	          AssistantC assistant = AiServices.builder(AssistantC.class)
	                  .chatLanguageModel(agent)
	                  .tools(restApiTool)
	                  .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
	                  .build();
	          // Use the assistant to make a query
	           response = assistant.chat(intermediateAnswer);
	          System.out.println(response);
	     /*  } else{
	    	  response =  intermediateAnswer; */
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
		              .documentSplitter(DocumentSplitters.recursive(2000, 200))
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
	    

		  /**
		   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
		   */
		  @MediaType(value = ANY, strict = false)
		  @Alias("EMBEDDING-get-info-from-store-legacy")  
		  public String promptFromEmbeddingLegacy(String storeName, String data, @Config LangchainLLMConfiguration configuration, @ParameterGroup(name= "Additional properties") LangchainLLMParameters LangchainParams) {
			  EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

		      InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
		      //EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		      
		      ChatLanguageModel model = createModel(configuration, LangchainParams);
		      
		      
		    //   ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(deserializedStore, embeddingModel);
		      
		    //   AssistantEmbedding assistant = AiServices.builder(AssistantEmbedding.class)
		    // 		    .chatLanguageModel(model)
		    // 		    .contentRetriever(contentRetriever)
		    // 		    .build();

		      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
		              .chatLanguageModel(model)
		              .retriever(EmbeddingStoreRetriever.from(deserializedStore, embeddingModel))
		              // .chatMemory() // you can override default chat memory
		              // .promptTemplate() // you can override default prompt template
		              .build();

		      String answer = chain.execute(data);
		      //String response = assistant.chat(data);
		      //System.out.println(answer); 

		      deserializedStore.serializeToFile(storeName);

			return answer;
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
	      String response = model.generate(data);
	      List<String> findURL = extractUrls(intermediateAnswer);
		  //System.out.println("find URL : " + findURL.get(0));
	      if (findURL!=null){
	    	  
		      //String name = chain.execute("What is the name from: " + intermediateAnswer + ". Reply only with the value.");
		      //String description = chain.execute("What is the description from: " + intermediateAnswer+ ". Reply only with the value.");
		      //String apiEndpoint = assistant.chat("What is the url from: " + intermediateAnswer+ ". Reply only with the value.");
		      //System.out.println("intermediate Answer: " + intermediateAnswer); 
		      //System.out.println("apiEndpoint: " + apiEndpoint); 
	
		      
		      // Create an instance of the custom tool with parameters
	          GenericRestApiTool restApiTool = new GenericRestApiTool(findURL.get(0), "API Call", "Execute GET or POST Requests");
		      
			  ChatLanguageModel agent = createModel(configuration, LangchainParams);
	        //   ChatLanguageModel agent = OpenAiChatModel.builder()
			// 	 	  .apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
			// 		  .modelName(LangchainParams.getModelName())
	        //           .temperature(0.1)
	        //           .timeout(ofSeconds(60))
	        //           .logRequests(true)
	        //           .logResponses(true)
	        //           .build();
	          // Build the assistant with the custom tool
	          AssistantC assistantC = AiServices.builder(AssistantC.class)
	                  .chatLanguageModel(agent)
	                  .tools(restApiTool)
	                  .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
	                  .build();
	          // Use the assistant to make a query
	           response = assistantC.chat(intermediateAnswer);
	          System.out.println(response);
	     /*  } else{
	    	  response =  intermediateAnswer; */
	      }
	      
	      
		return response;
	  }  

			

	    
}