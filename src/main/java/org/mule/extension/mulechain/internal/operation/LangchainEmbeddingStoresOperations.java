package org.mule.extension.mulechain.internal.operation;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import static org.mapdb.Serializer.STRING;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import dev.langchain4j.data.embedding.Embedding;
import static java.util.stream.Collectors.joining;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mule.extension.mulechain.internal.helpers.FileTypeParameters;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.tools.GenericRestApiTool;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainEmbeddingStoresOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainEmbeddingStoresOperations.class);

  private EmbeddingModel embeddingModel;

  private static InMemoryEmbeddingStore<TextSegment> deserializedStore;

  private static InMemoryEmbeddingStore<TextSegment> getDeserializedStore(String storeName, boolean getLatest) {
    if (deserializedStore == null || getLatest) {
      deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
    }
    return deserializedStore;
  }


  public LangchainEmbeddingStoresOperations() {
    this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
  }

  @MediaType(value = ANY, strict = false)
  @Alias("RAG-load-document")
  public String loadDocumentFile(String data, String contextPath, @ParameterGroup(name = "Context") FileTypeParameters fileType,
                                 @Config LangchainLLMConfiguration configuration) {

    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build();

    LOGGER.info("RAG loading document with file type: {}", fileType.getFileType());

    // ChatLanguageModel model = null;
    ingestDocument(fileType, contextPath, ingestor);

    ChatLanguageModel model = configuration.getModel();


    // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
    // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
    //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services

    ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);

    AssistantSources assistant = AiServices.builder(AssistantSources.class)
        .chatLanguageModel(model)
        .contentRetriever(contentRetriever)
        .build();

    Result<String> answer = assistant.chat(data);
    //    LOGGER.info("Answer: {}", answer);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", answer.content());
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put("inputCount", answer.tokenUsage().inputTokenCount());
    tokenUsage.put("outputCount", answer.tokenUsage().outputTokenCount());
    tokenUsage.put("totalCount", answer.tokenUsage().totalTokenCount());
    jsonObject.put("tokenUsage", tokenUsage);
    jsonObject.put("filePath", contextPath);
    jsonObject.put("fileType", fileType);
    jsonObject.put("question", data);

    return jsonObject.toString();
  }

  private void ingestDocument(FileTypeParameters fileType, String contextPath, EmbeddingStoreIngestor ingestor) {
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
          LOGGER.error("Error while loading the document: " + contextPath, e);
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
  }



  // interface Assistant {

  //   String chat(@MemoryId int memoryId, @UserMessage String userMessage);
  // }

  interface AssistantMemory {

    Result<String> chat(@MemoryId String memoryName, @UserMessage String userMessage);
  }



  /**
   * Implements a chat memory for a defined LLM as an AI Agent. The memoryName is allows the multi-channel / profile design.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("CHAT-answer-prompt-with-memory")
  public String chatWithPersistentMemory(String data, String memoryName, String dbFilePath, int maxMessages,
                                         @Config LangchainLLMConfiguration configuration) {

    ChatLanguageModel model = configuration.getModel();

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

    Result<String> response = assistant.chat(memoryName, data);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", response.content());
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put("inputCount", response.tokenUsage().inputTokenCount());
    tokenUsage.put("outputCount", response.tokenUsage().outputTokenCount());
    tokenUsage.put("totalCount", response.tokenUsage().totalTokenCount());
    jsonObject.put("tokenUsage", tokenUsage);

    jsonObject.put("memoryName", memoryName);
    jsonObject.put("dbFilePath", dbFilePath);
    jsonObject.put("maxMessages", maxMessages);


    return jsonObject.toString();

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
  public String useTools(String data, String toolConfig, @Config LangchainLLMConfiguration configuration) {

    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(30000, 200))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build();


    Document document = loadDocument(toolConfig, new TextDocumentParser());
    ingestor.ingest(document);


    ChatLanguageModel model = configuration.getModel();



    // MIGRATE CHAINS TO AI SERVICES: https://docs.langchain4j.dev/tutorials/ai-services/
    // and Specifically the RAG section: https://docs.langchain4j.dev/tutorials/ai-services#rag
    //chains are legacy now, please use AI Services: https://docs.langchain4j.dev/tutorials/ai-services > Update to AI Services
    ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
        .chatLanguageModel(model)
        .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
        // .chatMemory() // you can override default chat memory
        // .promptTemplate() // you can override default prompt template
        .build();


    boolean toolsUsed = false;
    String intermediateAnswer = chain.execute(data);
    String response = model.generate(data);
    List<String> findURL = extractUrls(intermediateAnswer);
    if (findURL != null) {

      toolsUsed = true;

      // Create an instance of the custom tool with parameters
      GenericRestApiTool restApiTool = new GenericRestApiTool(findURL.get(0), "API Call", "Execute GET or POST Requests");

      // Build the assistant with the custom tool
      AssistantC assistant = AiServices.builder(AssistantC.class)
          .chatLanguageModel(model)
          .tools(restApiTool)
          .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
          .build();
      // Use the assistant to make a query
      response = assistant.chat(intermediateAnswer);
      LOGGER.info("Response: {}", response);
      /*  } else{
        response =  intermediateAnswer; */
    }


    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", response);
    jsonObject.put("toolsUsed", toolsUsed);


    return jsonObject.toString();
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

    embeddingStore.serializeToFile(storeName);

    embeddingStore = null;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "created");

    return jsonObject.toString();
  }



  /**
   * Add document of type text, pdf and url to embedding store (in-memory), which is exported to the defined storeName (full path)
   */
  @MediaType(value = ANY, strict = false)
  @Alias("EMBEDDING-add-document-to-store")
  public String addFileEmbedding(String storeName, String contextPath,
                                 @ParameterGroup(name = "Context") FileTypeParameters fileType) {

    //EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();


    InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
    //EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(2000, 200))
        .embeddingModel(this.embeddingModel)
        .embeddingStore(deserializedStore)
        .build();

    //Document document = loadDocument(toPath("story-about-happy-carrot.txt"), new TextDocumentParser());

    //Document document = loadDocument(contextFile, new TextDocumentParser());
    //ingestor.ingest(document);



    // ChatLanguageModel model = null;

    ingestDocument(fileType, contextPath, ingestor);

    deserializedStore.serializeToFile(storeName);
    deserializedStore = null;

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("fileType", fileType.getFileType());
    jsonObject.put("filePath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");

    return jsonObject.toString();
  }


  /**
   * Query information from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = ANY, strict = false)
  @Alias("EMBEDDING-query-from-store")
  public String queryFromEmbedding(String storeName, String question, Number maxResults, Double minScore, boolean getLatest) {
    int maximumResults = (int) maxResults;
    if (minScore == null || minScore == 0) {
      minScore = 0.7;
    }

    //EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    //InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);

    InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);



    Embedding questionEmbedding = this.embeddingModel.embed(question).content();

    List<EmbeddingMatch<TextSegment>> relevantEmbeddings = store.findRelevant(questionEmbedding, maximumResults, minScore);

    String information = relevantEmbeddings.stream()
        .map(match -> match.embedded().text())
        .collect(joining("\n\n"));


    //deserializedStore = null;
    questionEmbedding = null;


    JSONObject jsonObject = new JSONObject();
    jsonObject.put("maxResults", maxResults);
    jsonObject.put("minScore", minScore);
    jsonObject.put("question", question);
    jsonObject.put("storeName", storeName);
    jsonObject.put("information", information);

    return jsonObject.toString();
  }



  /**
   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = ANY, strict = false)
  @Alias("EMBEDDING-get-info-from-store")
  public String promptFromEmbedding(String storeName, String data, boolean getLatest,
                                    @Config LangchainLLMConfiguration configuration) {

    InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);

    ChatLanguageModel model = configuration.getModel();


    ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(store, this.embeddingModel);

    Result<String> results;
    JSONObject jsonObject = new JSONObject();


    AssistantSources assistantSources = AiServices.builder(AssistantSources.class)
        .chatLanguageModel(model)
        .contentRetriever(contentRetriever)
        .build();


    results = assistantSources.chat(data);
    List<Content> contents = results.sources();

    jsonObject.put("response", results.content());
    jsonObject.put("storeName", storeName);
    jsonObject.put("question", data);
    jsonObject.put("getLatest", getLatest);
    JSONArray sources = new JSONArray();
    String absoluteDirectoryPath;
    String fileName;
    Metadata metadata;

    JSONObject contentObject;
    for (Content content : contents) {
      metadata = content.textSegment().metadata();
      absoluteDirectoryPath = (String) metadata.getString("absolute_directory_path");
      fileName = (String) metadata.getString("file_name");

      contentObject = new JSONObject();
      contentObject.put("absoluteDirectoryPath", absoluteDirectoryPath);
      contentObject.put("fileName", fileName);
      contentObject.put("textSegment", content.textSegment().text());
      sources.put(contentObject);
    }

    jsonObject.put("sources", sources);

    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put("inputCount", results.tokenUsage().inputTokenCount());
    tokenUsage.put("outputCount", results.tokenUsage().outputTokenCount());
    tokenUsage.put("totalCount", results.tokenUsage().totalTokenCount());
    jsonObject.put("tokenUsage", tokenUsage);

    return jsonObject.toString();
  }


  interface AssistantSources {

    Result<String> chat(String userMessage);
  }



  /**
   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = ANY, strict = false)
  @Alias("EMBEDDING-get-info-from-store-legacy")
  public String promptFromEmbeddingLegacy(String storeName, String data, boolean getLatest,
                                          @Config LangchainLLMConfiguration configuration) {
    InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);

    ChatLanguageModel model = configuration.getModel();



    ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
        .chatLanguageModel(model)
        .retriever(EmbeddingStoreRetriever.from(store, this.embeddingModel))
        .build();

    String answer = chain.execute(data);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", answer);
    jsonObject.put("storeName", storeName);
    jsonObject.put("getLatest", getLatest);


    return jsonObject.toString();
  }


  interface AssistantEmbedding {

    String chat(String userMessage);
  }


  /**
  * (AI Services) Usage of tools by a defined AI Agent. Provide a list of tools (APIs) with all required informations (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
  */
  @MediaType(value = ANY, strict = false)
  @Alias("TOOLS-use-ai-service")
  public String useAIServiceTools(String data, String toolConfig, @Config LangchainLLMConfiguration configuration) {

    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(30000, 200))
        .embeddingModel(embeddingModel)
        .embeddingStore(embeddingStore)
        .build();


    Document document = loadDocument(toolConfig, new TextDocumentParser());
    ingestor.ingest(document);


    ChatLanguageModel model = configuration.getModel();


    ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);


    AssistantEmbedding assistant = AiServices.builder(AssistantEmbedding.class)
        .chatLanguageModel(model)
        .contentRetriever(contentRetriever)
        .build();

    boolean toolsUsed = false;

    String intermediateAnswer = assistant.chat(data);
    String response = model.generate(data);
    List<String> findURL = extractUrls(intermediateAnswer);
    //System.out.println("find URL : " + findURL.get(0));
    if (findURL != null) {

      toolsUsed = true;
      // Create an instance of the custom tool with parameters
      GenericRestApiTool restApiTool = new GenericRestApiTool(findURL.get(0), "API Call", "Execute GET or POST Requests");

      // Build the assistant with the custom tool
      AssistantC assistantC = AiServices.builder(AssistantC.class)
          .chatLanguageModel(model)
          .tools(restApiTool)
          .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
          .build();
      // Use the assistant to make a query
      response = assistantC.chat(intermediateAnswer);
      LOGGER.info("Response: {}", response);
      /*  } else{
        response =  intermediateAnswer; */
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("response", response);
    jsonObject.put("toolsUsed", toolsUsed);


    return jsonObject.toString();
  }


  /**
  * Add document of type text, pdf and url to embedding store (in-memory), which is exported to the defined storeName (full path)
  */
  @MediaType(value = ANY, strict = false)
  @Alias("EMBEDDING-add-folder-to-store")
  public String addFilesFromFolderEmbedding(String storeName, String contextPath,
                                            @ParameterGroup(name = "Context") FileTypeParameters fileType) {

    //EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);

    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(2000, 200))
        .embeddingModel(this.embeddingModel)
        .embeddingStore(deserializedStore)
        .build();


    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(contextPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      LOGGER.error("Unable to load files in the path: " + contextPath, e);
    }

    LOGGER.info("Total number of files to process: {}", totalFiles);
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(contextPath))) {
      paths.filter(Files::isRegularFile).forEach(file -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        LOGGER.info("Processing file {}: {}", currentFileCounter, file.getFileName());
        Document document = null;
        try {
          switch (fileType.getFileType()) {
            case "text":
              document = loadDocument(file.toString(), new TextDocumentParser());
              ingestor.ingest(document);
              break;
            case "pdf":
              document = loadDocument(file.toString(), new ApacheTikaDocumentParser());
              ingestor.ingest(document);
              break;
            case "url":
              // Handle URLs separately if needed
              break;
            default:
              throw new IllegalArgumentException("Unsupported File Type: " + fileType.getFileType());
          }
        } catch (BlankDocumentException e) {
          LOGGER.warn("Skipping file due to BlankDocumentException: {}", file.getFileName());
        }
      });
    } catch (IOException e) {
      LOGGER.error("Exception occurred while loading files: " + contextPath, e);
    }



    deserializedStore.serializeToFile(storeName);
    deserializedStore = null;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("filesCount", totalFiles);
    jsonObject.put("folderPath", contextPath);
    jsonObject.put("storeName", storeName);
    jsonObject.put("status", "updated");


    return jsonObject.toString();
  }

}
