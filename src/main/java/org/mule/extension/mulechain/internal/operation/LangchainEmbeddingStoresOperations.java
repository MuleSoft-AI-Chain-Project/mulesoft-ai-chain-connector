/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import dev.langchain4j.data.embedding.Embedding;
import static java.util.stream.Collectors.joining;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mule.extension.mulechain.api.metadata.LLMResponseAttributes;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.extension.mulechain.internal.error.provider.EmbeddingErrorTypeProvider;
import org.mule.extension.mulechain.internal.helpers.FileType;
import org.mule.extension.mulechain.internal.helpers.FileTypeParameters;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.tools.GenericRestApiTool;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
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
import static org.mule.extension.mulechain.internal.helpers.ResponseHelper.createLLMResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainEmbeddingStoresOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainEmbeddingStoresOperations.class);

  private final EmbeddingModel embeddingModel;

  private InMemoryEmbeddingStore<TextSegment> deserializedStore;

  private InMemoryEmbeddingStore<TextSegment> getDeserializedStore(String storeName, boolean getLatest) {
    if (deserializedStore == null || getLatest) {
      deserializedStore = InMemoryEmbeddingStore.fromFile(storeName);
    }
    return deserializedStore;
  }


  public LangchainEmbeddingStoresOperations() {
    this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("RAG-load-document")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> loadDocumentFile(@Config LangchainLLMConfiguration configuration,
                                                                                                                      @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                      String contextPath,
                                                                                                                      @ParameterGroup(
                                                                                                                          name = "Context") FileTypeParameters fileType) {

    try {
      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      LOGGER.info("RAG loading document with file type: {}", fileType.getFileType());

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

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, answer.content());

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.FILE_PATH, contextPath);
      attributes.put(MuleChainConstants.FILE_TYPE, fileType.getFileType());
      attributes.put(MuleChainConstants.QUESTION, data);

      return createLLMResponse(jsonObject.toString(), answer, attributes);
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw new ModuleException("Error while loading and retrieving content from the document " + contextPath,
                                MuleChainErrorType.RAG_FAILURE, e);
    }
  }

  private void ingestDocument(FileTypeParameters fileType, String contextPath, EmbeddingStoreIngestor ingestor) {
    Document document = null;
    switch (FileType.fromValue(fileType.getFileType())) {
      case TEXT:
        document = loadDocument(contextPath, new TextDocumentParser());
        ingestor.ingest(document);
        break;
      case PDF:
        document = loadDocument(contextPath, new ApacheTikaDocumentParser());
        ingestor.ingest(document);
        break;
      case URL:
        URL url = null;
        try {
          url = new URL(contextPath);
        } catch (MalformedURLException e) {
          throw new ModuleException("Error while loading the document: " + contextPath, MuleChainErrorType.FILE_HANDLING_FAILURE,
                                    e);
        }

        Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
        HtmlTextExtractor transformer = new HtmlTextExtractor(null, null, true);
        document = transformer.transform(htmlDocument);
        document.metadata().add("url", contextPath);
        ingestor.ingest(document);
        break;
      default:
        throw new ModuleException("Unsupported File Type: " + fileType.getFileType(), MuleChainErrorType.FILE_HANDLING_FAILURE);
    }
  }

  interface AssistantMemory {

    Result<String> chat(@MemoryId String memoryName, @UserMessage String userMessage);
  }



  /**
   * Implements a chat memory for a defined LLM as an AI Agent. The memoryName is allows the multi-channel / profile design.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("CHAT-answer-prompt-with-memory")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> chatWithPersistentMemory(@Config LangchainLLMConfiguration configuration,
                                                                                                                              @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                              String memoryName,
                                                                                                                              String dbFilePath,
                                                                                                                              int maxMessages) {

    try {
      ChatLanguageModel model = configuration.getModel();
      PersistentChatMemoryStore store = new PersistentChatMemoryStore(dbFilePath);
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
      jsonObject.put(MuleChainConstants.RESPONSE, response.content());

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.MEMORY_NAME, memoryName);
      attributes.put(MuleChainConstants.DB_FILE_PATH, dbFilePath);
      attributes.put(MuleChainConstants.MAX_MESSAGES, maxMessages);

      return createLLMResponse(jsonObject.toString(), response, attributes);
    } catch (Exception e) {
      throw new ModuleException("Error while responding with the chat provided", MuleChainErrorType.AI_SERVICES_FAILURE, e);
    }
  }

  static class PersistentChatMemoryStore implements ChatMemoryStore {

    private final DB db;
    private final Map<String, String> map;

    public PersistentChatMemoryStore(String dbMFilePath) {
      db = DBMaker.fileDB(dbMFilePath)
          .transactionEnable()
          .fileLockDisable()
          .make();
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
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("TOOLS-use-ai-service-legacy")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> useTools(@Config LangchainLLMConfiguration configuration,
                                                                                                            @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                            String toolConfig) {

    try {
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

      String intermediateAnswer = chain.execute(data);
      String response = model.generate(data);
      List<String> findURLs = extractUrls(intermediateAnswer);
      boolean toolsUsed = false;

      if (findURLs != null) {

        toolsUsed = true;

        // Create an instance of the custom tool with parameters
        GenericRestApiTool restApiTool = new GenericRestApiTool(findURLs.get(0), "API Call", "Execute GET or POST Requests");

        // Build the assistant with the custom tool
        AssistantC assistant = AiServices.builder(AssistantC.class)
            .chatLanguageModel(model)
            .tools(restApiTool)
            //.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
        // Use the assistant to make a query
        response = assistant.chat(intermediateAnswer);
        LOGGER.info("Response: {}", response);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, response);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.TOOLS_USED, toolsUsed);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error occurred while executing AI Tools with the provided config",
                                MuleChainErrorType.TOOLS_OPERATION_FAILURE, e);
    }
  }


  interface AssistantC {

    String chat(String userMessage);
  }



  //************ IMPORTANT ******************//

  // TO DO TASKS SERIALIZATION AND DESERIALIZATION FOR STORE
  // In-memory embedding store can be serialized and deserialized to/from file

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
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-new-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> createEmbedding(String storeName) {
    try {
      InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

      embeddingStore.serializeToFile(storeName);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.STATUS, MuleChainConstants.CREATED);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.STORE_NAME, storeName);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error while creating new Embedding store: " + storeName,
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }



  /**
   * Add document of type text, pdf and url to embedding store (in-memory), which is exported to the defined storeName (full path)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-document-to-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> addFileEmbedding(String storeName,
                                                                                                                    String contextPath,
                                                                                                                    @ParameterGroup(
                                                                                                                        name = "Context") FileTypeParameters fileType) {

    try {
      InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(storeName);

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(2000, 200))
          .embeddingModel(this.embeddingModel)
          .embeddingStore(store)
          .build();

      ingestDocument(fileType, contextPath, ingestor);

      store.serializeToFile(storeName);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.STATUS, MuleChainConstants.UPDATED);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.FILE_TYPE, fileType.getFileType());
      attributes.put(MuleChainConstants.FILE_PATH, contextPath);
      attributes.put(MuleChainConstants.STORE_NAME, storeName);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw new ModuleException(String.format("Error while adding document %s to the Embedding store %s",
                                              contextPath, storeName),
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE,
                                e);
    }
  }


  /**
   * Query information from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-query-from-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> queryFromEmbedding(String storeName,
                                                                                                                      @org.mule.runtime.extension.api.annotation.param.Content String question,
                                                                                                                      int maxResults,
                                                                                                                      double minScore,
                                                                                                                      boolean getLatest) {
    try {
      if (minScore == 0) {
        minScore = 0.7;
      }

      InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);

      Embedding questionEmbedding = this.embeddingModel.embed(question).content();

      List<EmbeddingMatch<TextSegment>> relevantEmbeddings = store.findRelevant(questionEmbedding, maxResults, minScore);

      String information = relevantEmbeddings.stream()
          .map(match -> match.embedded().text())
          .collect(joining("\n\n"));

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, information);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.MAX_RESULTS, maxResults);
      attributes.put(MuleChainConstants.MIN_SCORE, minScore);
      attributes.put(MuleChainConstants.QUESTION, question);
      attributes.put(MuleChainConstants.STORE_NAME, storeName);

      JSONArray sources = new JSONArray();
      String absoluteDirectoryPath;
      String fileName;
      String url;

      JSONObject contentObject;
      String fullPath;
      for (EmbeddingMatch<TextSegment> match : relevantEmbeddings) {
        Metadata matchMetadata = match.embedded().metadata();

        fileName = matchMetadata.getString(MuleChainConstants.EmbeddingConstants.FILE_NAME);
        url = matchMetadata.getString(MuleChainConstants.URL);
        fullPath = matchMetadata.getString(MuleChainConstants.EmbeddingConstants.FULL_PATH);
        absoluteDirectoryPath = matchMetadata.getString(MuleChainConstants.EmbeddingConstants.ABSOLUTE_DIRECTORY_PATH);

        contentObject = new JSONObject();
        contentObject.put(MuleChainConstants.ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
        contentObject.put(MuleChainConstants.FULL_PATH, fullPath);
        contentObject.put(MuleChainConstants.FILE_NAME, fileName);
        contentObject.put(MuleChainConstants.URL, url);
        contentObject.put(MuleChainConstants.INDIVIDUAL_SCORE, match.score());
        contentObject.put(MuleChainConstants.TEXT_SEGMENT, match.embedded().text());

        sources.put(contentObject);
      }
      attributes.put(MuleChainConstants.SOURCES, sources);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error while querying from the embedding store " + storeName,
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }

  /**
   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-get-info-from-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> promptFromEmbedding(@Config LangchainLLMConfiguration configuration,
                                                                                                                         @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                         String storeName,
                                                                                                                         boolean getLatest) {

    try {
      InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);

      ChatLanguageModel model = configuration.getModel();

      ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(store, this.embeddingModel);

      AssistantSources assistantSources = AiServices.builder(AssistantSources.class)
          .chatLanguageModel(model)
          .contentRetriever(contentRetriever)
          .build();

      Result<String> results;
      results = assistantSources.chat(data);
      List<Content> contents = results.sources();

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, results.content());

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.STORE_NAME, storeName);
      attributes.put(MuleChainConstants.QUESTION, data);
      attributes.put(MuleChainConstants.GET_LATEST, getLatest);

      JSONArray sources = new JSONArray();
      String absoluteDirectoryPath;
      String fileName;
      String url;
      Metadata metadata;

      JSONObject contentObject;
      for (Content content : contents) {
        metadata = content.textSegment().metadata();
        absoluteDirectoryPath = metadata.getString(MuleChainConstants.EmbeddingConstants.ABSOLUTE_DIRECTORY_PATH);
        fileName = metadata.getString(MuleChainConstants.EmbeddingConstants.FILE_NAME);
        url = metadata.getString(MuleChainConstants.URL);

        contentObject = new JSONObject();
        contentObject.put(MuleChainConstants.ABSOLUTE_DIRECTORY_PATH, absoluteDirectoryPath);
        contentObject.put(MuleChainConstants.FILE_NAME, fileName);
        contentObject.put(MuleChainConstants.URL, url);
        contentObject.put(MuleChainConstants.TEXT_SEGMENT, content.textSegment().text());
        sources.put(contentObject);
      }
      attributes.put(MuleChainConstants.SOURCES, sources);

      return createLLMResponse(jsonObject.toString(), results, attributes);
    } catch (Exception e) {
      throw new ModuleException(String.format("Error while getting info from the store %s", storeName),
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }

  interface AssistantSources {

    Result<String> chat(String userMessage);
  }

  /**
   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-get-info-from-store-legacy")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> promptFromEmbeddingLegacy(@Config LangchainLLMConfiguration configuration,
                                                                                                                             @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                             String storeName,
                                                                                                                             boolean getLatest) {
    try {
      InMemoryEmbeddingStore<TextSegment> store = getDeserializedStore(storeName, getLatest);

      ChatLanguageModel model = configuration.getModel();

      ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
          .chatLanguageModel(model)
          .retriever(EmbeddingStoreRetriever.from(store, this.embeddingModel))
          .build();

      String answer = chain.execute(data);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, answer);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.STORE_NAME, storeName);
      attributes.put(MuleChainConstants.GET_LATEST, getLatest);


      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException(String.format("Error while getting info from the store %s", storeName),
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }


  interface AssistantEmbedding {

    String chat(String userMessage);
  }


  /**
  * (AI Services) Usage of tools by a defined AI Agent. Provide a list of tools (APIs) with all required informations (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
  */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("TOOLS-use-ai-service")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> useAIServiceTools(@Config LangchainLLMConfiguration configuration,
                                                                                                                     @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                     String toolConfig) {
    try {
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

      String intermediateAnswer = assistant.chat(data);
      String response = model.generate(data);
      List<String> findURLs = extractUrls(intermediateAnswer);
      boolean toolsUsed = false;

      if (findURLs != null) {

        toolsUsed = true;
        // Create an instance of the custom tool with parameters
        GenericRestApiTool restApiTool = new GenericRestApiTool(findURLs.get(0), "API Call", "Execute GET or POST Requests");

        // Build the assistant with the custom tool
        AssistantC assistantC = AiServices.builder(AssistantC.class)
            .chatLanguageModel(model)
            .tools(restApiTool)
            //.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
        // Use the assistant to make a query
        response = assistantC.chat(intermediateAnswer);
        LOGGER.info("Response: {}", response);
      }

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.RESPONSE, response);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.TOOLS_USED, toolsUsed);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error occurred while executing AI Tools with the provided config",
                                MuleChainErrorType.TOOLS_OPERATION_FAILURE, e);
    }
  }


  /**
  * Add document of type text, pdf and url to embedding store (in-memory), which is exported to the defined storeName (full path)
  */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-folder-to-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> addFilesFromFolderEmbedding(String storeName,
                                                                                                                               String contextPath,
                                                                                                                               @ParameterGroup(
                                                                                                                                   name = "Context") FileTypeParameters fileType) {
    try {
      InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(storeName);

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(2000, 200))
          .embeddingModel(this.embeddingModel)
          .embeddingStore(store)
          .build();

      long totalFiles = getTotalFilesCount(contextPath);
      ingestFolder(contextPath, fileType, ingestor);
      store.serializeToFile(storeName);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.STATUS, MuleChainConstants.UPDATED);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.FILES_COUNT, totalFiles);
      attributes.put(MuleChainConstants.FOLDER_PATH, contextPath);
      attributes.put(MuleChainConstants.STORE_NAME, storeName);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (ModuleException e) {
      throw e;
    } catch (Exception e) {
      throw new ModuleException(String.format("Error while adding folder %s into the store %s", contextPath,
                                              storeName),
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE,
                                e);
    }
  }

  private long getTotalFilesCount(String contextPath) {
    long totalFiles = 0;
    try (Stream<Path> paths = Files.walk(Paths.get(contextPath))) {
      totalFiles = paths.filter(Files::isRegularFile).count();
    } catch (IOException e) {
      LOGGER.error("Unable to load files in the path: " + contextPath, e);
    }

    LOGGER.info("Total number of files to process: {}", totalFiles);
    return totalFiles;
  }

  private void ingestFolder(String contextPath, FileTypeParameters fileType, EmbeddingStoreIngestor ingestor) {
    AtomicInteger fileCounter = new AtomicInteger(0);
    try (Stream<Path> paths = Files.walk(Paths.get(contextPath))) {
      paths.filter(Files::isRegularFile).forEach(file -> {
        int currentFileCounter = fileCounter.incrementAndGet();
        LOGGER.info("Processing file {}: {}", currentFileCounter, file.getFileName());
        Document document = null;
        try {
          switch (FileType.fromValue(fileType.getFileType())) {
            case TEXT:
              document = loadDocument(file.toString(), new TextDocumentParser());
              ingestor.ingest(document);
              break;
            case PDF:
              document = loadDocument(file.toString(), new ApacheTikaDocumentParser());
              ingestor.ingest(document);
              break;
            case URL:
              // Handle URLs separately if needed
              break;
            default:
              throw new ModuleException("Unsupported File Type: " + fileType.getFileType(),
                                        MuleChainErrorType.FILE_HANDLING_FAILURE);
          }
        } catch (BlankDocumentException e) {
          LOGGER.warn("Skipping file due to BlankDocumentException: {}", file.getFileName());
        }
      });
    } catch (IOException e) {
      throw new ModuleException("Exception occurred while loading files: " + contextPath,
                                MuleChainErrorType.FILE_HANDLING_FAILURE, e);
    }
  }

}
