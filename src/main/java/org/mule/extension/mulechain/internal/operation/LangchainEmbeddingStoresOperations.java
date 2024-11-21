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
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;
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
import org.mule.extension.mulechain.internal.operation.LangchainEmbeddingStoresOperations.AssistantSources;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.tools.GenericRestApiTool;
import org.mule.extension.mulechain.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.output.Response;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is a container for embedding related operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainEmbeddingStoresOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainEmbeddingStoresOperations.class);

  @ExcludeFromGeneratedCoverage
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

  /**
   * Enables the user to query the doc provided in natural language.<br>
   * The doc will be embedded into in memory vector store.
   *
   * @param configuration       Refers to the configuration object
   * @param data                Defines the query input provided by the user
   * @param contextPath         Defines the file path which will be embedded
   * @param fileType            Specifies the type of file. {@link org.mule.extension.mulechain.internal.helpers.FileType} Eg: "any", "text" & "url"
   * @return                    Returns the output response of the query
   */
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
      LOGGER.debug("RAG Load Document Operation called with data: {}, file: {} & fileType: {}", data, contextPath,
                   fileType.getFileType());
      EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(1000, 200, new OpenAiTokenizer()))
          .embeddingModel(embeddingModel)
          .embeddingStore(embeddingStore)
          .build();

      ingestDocument(fileType, contextPath, ingestor);

      LOGGER.debug("File successfully embedded into the in-memory embedding store");

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

      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.FILE_PATH, contextPath);
      attributes.put(MuleChainConstants.FILE_TYPE, fileType.getFileType());
      attributes.put(MuleChainConstants.QUESTION, data);

      LOGGER.debug("RAG Load Document Operation completed with response: {}", answer.content());

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
      case ANY:
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

        //Document htmlDocument = UrlDocumentLoader.load(url, new TextDocumentParser());
        document = UrlDocumentLoader.load(url, new TextDocumentParser());

        /*HtmlToTextDocumentTransformer transformer = new HtmlToTextDocumentTransformer(null, null, true);
        document = transformer.transform(htmlDocument);*/
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
   * Implements a chat memory for a defined LLM as an AI Agent. The memoryName allows the multichannel / profile design.
   * @param configuration           Refers to the configuration object
   * @param data                    Refers to the user prompt
   * @param memoryName              Name of the memory to be fetched for further processing by the LLMs
   * @param dbFilePath              Location of the file containing the memory
   * @param maxMessages             Max messages to be analyzed for that memory.
   * @return                        Returns the response as sent by the LLM
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
      LOGGER.debug(
                   "Chat Answer Prompt With Memory Operation called with userPrompt: {}, memoryName: {}, dbFilePath: {} & maxMessages: {}",
                   data, memoryName, dbFilePath, maxMessages);
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

      LOGGER.debug("Chat Answer Prompt With Memory Operation completed with response: {}", response.content());

      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.MEMORY_NAME, memoryName);
      attributes.put(MuleChainConstants.DB_FILE_PATH, dbFilePath);
      attributes.put(MuleChainConstants.MAX_MESSAGES, String.valueOf(maxMessages));

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

  interface AssistantR {

    Result<String> chat(String userMessage);
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
   * @param storeName           Name of the embedding store
   * @return                    Returns the status of creation of the store.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-new-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> createEmbedding(String storeName) {
    try {
      LOGGER.debug("Embedding New Store Operation called with the storeName: {}", storeName);
      InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

      embeddingStore.serializeToFile(storeName);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.STATUS, MuleChainConstants.CREATED);

      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.STORE_NAME, storeName);

      LOGGER.debug("Embedding New Store Operation completed with {} creation", storeName);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error while creating new Embedding store: " + storeName,
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }


  /**
   * Add document of type text, any and url to embedding store (in-memory), which is exported to the defined storeName (full path)
   * @param storeName               Name of the embedding store
   * @param contextPath             Refers to the location of the file to be processed
   * @param maxSegmentSizeInChars   Max allowed size of continuous sequence of characters while embedding
   * @param maxOverlapSizeInChars   Max size of overlapping characters allowed while embedding
   * @param fileType                Refers to the type of the file (any, text, url)
   * @return                        Returns the status of the embedding operation
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-document-to-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> addFileEmbedding(String storeName,
                                                                                                                    String contextPath,
                                                                                                                    int maxSegmentSizeInChars,
                                                                                                                    int maxOverlapSizeInChars,
                                                                                                                    @ParameterGroup(
                                                                                                                        name = "Context") FileTypeParameters fileType) {

    try {
      LOGGER.debug("Embedding Add Document To Store Operation called with the storeName: {}, filePath: {} & fileType: {}",
                   storeName, contextPath, fileType.getFileType());
      InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(storeName);

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
          .embeddingModel(this.embeddingModel)
          .embeddingStore(store)
          .build();

      ingestDocument(fileType, contextPath, ingestor);

      store.serializeToFile(storeName);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MuleChainConstants.STATUS, MuleChainConstants.UPDATED);

      LOGGER.debug("File ({}) successfully ingested into the store: {}", contextPath, storeName);

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
   * @param storeName         Name of the embedding store
   * @param question          Refers to the user prompt or query
   * @param maxResults        Max results to be retrieved from the store
   * @param minScore          Filters the response with this minScore
   * @param getLatest         Determines whether the store needs to be freshly fetched from the location
   * @return                  Returns the relevant embeddings with the attached sources
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
      LOGGER.debug(
                   "Embedding Query from Store Operation called with storeName: {}, latestFetchRequired:{}, query: {}, minScore: {}, maxResults: {}",
                   storeName, getLatest, question, minScore, maxResults);
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

      LOGGER.debug("Embedding Query from Store Operation completed with the information: {}", information);

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
      LOGGER.debug("Sources for the information: {}", sources);
      jsonObject.put(MuleChainConstants.SOURCES, sources);

      return createLLMResponse(jsonObject.toString(), attributes);
    } catch (Exception e) {
      throw new ModuleException("Error while querying from the embedding store " + storeName,
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }

  /**
   * Reads information via prompt from embedding store (in-Memory), which is imported from the storeName (full path)
   * @param configuration           Refers to the configuration object
   * @param data                    Refers to the user prompt or query
   * @param storeName               Name of the store to be queried
   * @param getLatest               Determines whether the store needs to be freshly fetched from the location
   * @return                        Returns the embeddings output by the LLM along with the sources
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
      LOGGER.debug("Embedding Get info from Store Operation called with storeName: {}, latestFetchRequired:{} & query: {}",
                   storeName, getLatest, data);
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

      LOGGER.debug("Embedding Get info from Store Operation completed with response: {}", results.content());

      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.STORE_NAME, storeName);
      attributes.put(MuleChainConstants.QUESTION, data);
      attributes.put(MuleChainConstants.GET_LATEST, String.valueOf(getLatest));

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
      jsonObject.put(MuleChainConstants.SOURCES, sources);
      LOGGER.debug("Sources for this information: {}", sources);

      return createLLMResponse(jsonObject.toString(), results, attributes);
    } catch (Exception e) {
      throw new ModuleException(String.format("Error while getting info from the store %s", storeName),
                                MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, e);
    }
  }

  interface AssistantSources {

    Result<String> chat(String userMessage);
  }


  interface AssistantEmbeddingR {

    Result<String> chat(String userMessage);
  }



  interface AssistantEmbeddingChat {

    Result<String> chat(String userMessage);
  }


  /**
   * (AI Services) Usage of tools by a defined AI Agent.<br>
   * Provide a list of tools (APIs) with all required information (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
   * @param configuration           Refers to the configuration object
   * @param data                    Refers to the user prompt or query
   * @param toolConfig              Contains the configuration required by the LLM to enable calling tools
   * @return                        Returns the response while considering tools configuration
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("TOOLS-use-ai-service")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/Response.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> useAIServiceTools(@Config LangchainLLMConfiguration configuration,
                                                                                                                       @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                       String toolConfig) {
    try {
      LOGGER.debug("Tools Use Ai Service Operation called with userPrompt: {}", data);
      LOGGER.debug("Tools Config: {}", toolConfig);
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

      AssistantEmbeddingR assistant = AiServices.builder(AssistantEmbeddingR.class)
          .chatLanguageModel(model)
          .contentRetriever(contentRetriever)
          .build();


      AssistantEmbeddingChat assistantChat = AiServices.builder(AssistantEmbeddingChat.class)
          .chatLanguageModel(model)
          .build();

      //String intermediateAnswer = assistant.chat(data);
      dev.langchain4j.service.Result<String> intermediateAnswer = assistant.chat(data);
      LOGGER.debug("Intermediate Answer containing the request URLs: {}", intermediateAnswer.content());
      //String response = model.generate(data);
      Result<String> response = assistantChat.chat(data);

      List<String> findURLs = extractUrls(intermediateAnswer.content());
      boolean toolsUsed = false;
      if (findURLs != null) {

        toolsUsed = true;
        // Create an instance of the custom tool with parameters
        GenericRestApiTool restApiTool = new GenericRestApiTool(findURLs.get(0), "API Call", "Execute GET or POST Requests");

        // Build the assistant with the custom tool
        AssistantR assistantC = AiServices.builder(AssistantR.class)
            .chatLanguageModel(model)
            .tools(restApiTool)
            //.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
        // Use the assistant to make a query
        //response = assistantC.chat(intermediateAnswer.content());
        response = assistantC.chat(intermediateAnswer.content());
        LOGGER.debug("Response after Tools Usage: {}", response.content());
      }

      JSONObject jsonObject = new JSONObject();

      jsonObject.put(MuleChainConstants.RESPONSE, response.content());


      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.TOOLS_USED, String.valueOf(toolsUsed));

      LOGGER.debug("Tools Use Ai Service Operation completed with response: {}, toolsUsed: {}", response, toolsUsed);
      return createLLMResponse(jsonObject.toString(), response, attributes);
    } catch (Exception e) {
      throw new ModuleException("Error occurred while executing AI Tools with the provided config",
                                MuleChainErrorType.TOOLS_OPERATION_FAILURE, e);
    }
  }


  /**
   * (AI Services) Usage of tools by a defined AI Agent.<br>
   * Provide a list of tools (APIs) with all required information (endpoint, headers, body, method, etc.) to the AI Agent to use it on purpose.
   * @param configuration           Refers to the configuration object
   * @param data                    Refers to the user prompt or query
   * @param toolConfig              Contains the configuration required by the LLM to enable calling tools
   * @return                        Returns the response while considering tools configuration
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("TOOLS-use-ai-native")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/ResponseTools.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, LLMResponseAttributes> useNativeAIServiceTools(@Config LangchainLLMConfiguration configuration,
                                                                                                                             @org.mule.runtime.extension.api.annotation.param.Content String data,
                                                                                                                             InputStream toolsArray) {
    try {
      LOGGER.debug("Tools Use Ai Service Operation called with userPrompt: {}", data);
      LOGGER.debug("Tools Config: {}", toolsArray);

      JSONArray tools = getInputString(toolsArray);

      List<ToolSpecification> toolsSpecs = getTools(tools, configuration);

      ChatLanguageModel model = configuration.getModel();

      dev.langchain4j.data.message.UserMessage userMessage = dev.langchain4j.data.message.UserMessage.from(data);


      Response<AiMessage> result = model.generate(Arrays.asList(userMessage), toolsSpecs);

      JSONObject jsonObject = new JSONObject();

      JSONArray toolExecutionRequests = new JSONArray();

      if (result.content().hasToolExecutionRequests()) {
        for (ToolExecutionRequest request : result.content().toolExecutionRequests()) {
          JSONObject itemJson = new JSONObject();
          String id = request.id();
          String name = request.name();
          String arguments = request.arguments();
          JSONObject argumentsJson = new JSONObject(arguments);
          itemJson.put("id", id);
          itemJson.put("name", name);
          itemJson.put("arguments", argumentsJson);
          toolExecutionRequests.put(itemJson);
        }
        jsonObject.put(MuleChainConstants.RESPONSE, "Tools were used");
        jsonObject.put(MuleChainConstants.TOOL_EXECUTION_REQUESTS, toolExecutionRequests);
      } else {
        JSONObject itemJson = new JSONObject();
        itemJson.put("id", "-");
        itemJson.put("name", "-");
        itemJson.put("arguments", "-");
        toolExecutionRequests.put(itemJson);
        jsonObject.put(MuleChainConstants.RESPONSE, result.content().text());
        jsonObject.put(MuleChainConstants.TOOL_EXECUTION_REQUESTS, toolExecutionRequests);
      }

      Map<String, String> attributes = new HashMap<>();
      attributes.put(MuleChainConstants.TOOLS_USED, String.valueOf(result.content().hasToolExecutionRequests()));
      return createLLMResponse(jsonObject.toString(), result, attributes);
    } catch (Exception e) {
      throw new ModuleException("Error occurred while executing native AI Tools with the provided config",
                                MuleChainErrorType.TOOLS_OPERATION_FAILURE, e);
    }
  }



  /**
   * Add document of type text, any and url to embedding store (in-memory), which is exported to the defined storeName (full path)
   * @param storeName                 Name of the embedding store
   * @param contextPath               Refers to the location of the folder to be processed
   * @param maxSegmentSizeInChars     Max allowed size of continuous sequence of characters while embedding
   * @param maxOverlapSizeInChars     Max size of overlapping characters allowed while embedding
   * @param fileType                  Refers to the type of the file (any, text) - url is not supported
   * @return                          Returns the status of the embedding operation
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-add-folder-to-store")
  @Throws(EmbeddingErrorTypeProvider.class)
  @OutputJsonType(schema = "api/response/StatusResponse.json")
  public org.mule.runtime.extension.api.runtime.operation.Result<InputStream, Map<String, Object>> addFilesFromFolderEmbedding(String storeName,
                                                                                                                               String contextPath,
                                                                                                                               int maxSegmentSizeInChars,
                                                                                                                               int maxOverlapSizeInChars,
                                                                                                                               @ParameterGroup(
                                                                                                                                   name = "Context") FileTypeParameters fileType) {
    try {
      LOGGER.debug("Embedding Add Folder To Store Operation called with storeName: {}, filePath: {} & fileType: {}", storeName,
                   contextPath, fileType.getFileType());
      InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromFile(storeName);

      EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars))
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

      LOGGER.debug("Embedding Add Folder To Store Operation completed successfully");

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
            case ANY:
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


  private static JSONArray getInputString(InputStream inputString) throws IOException {
    InputStreamReader reader = new InputStreamReader(inputString);
    StringBuilder inputStringBuilder = new StringBuilder();
    int c;
    while ((c = reader.read()) != -1) {
      inputStringBuilder.append((char) c);
    }
    return new JSONArray(inputStringBuilder.toString());

  }

  private static List<ToolSpecification> getToolsMistral(JSONArray tools, LangchainLLMConfiguration configuration) {
    List<ToolSpecification> toolSpecifications = new ArrayList<>();

    for (int i = 0; i < tools.length(); i++) {
      JSONObject functionEntry = tools.getJSONObject(i);
      JSONObject function = functionEntry.getJSONObject("function");

      String functionName = function.getString("name");
      String functionDescription = function.getString("description");

      JSONObject parameters = function.getJSONObject("parameters");
      JSONObject properties = parameters.getJSONObject("properties");

      Map<String, Map<String, Object>> propertiesMap = new HashMap<>();
      List<String> requiredProperties = new ArrayList<>();

      for (String propertyName : properties.keySet()) {
        JSONObject propertyDetails = properties.getJSONObject(propertyName);

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("type", propertyDetails.getString("type"));
        propertyMap.put("description", propertyDetails.getString("description"));

        // Add enum if present
        if (propertyDetails.has("enum")) {
          propertyMap.put("enum", propertyDetails.getJSONArray("enum").toList());
        }

        propertiesMap.put(propertyName, propertyMap);
      }

      // Gather required properties explicitly as an array
      if (parameters.has("required")) {
        JSONArray requiredArray = parameters.getJSONArray("required");
        requiredProperties = requiredArray.toList().stream()
            .map(Object::toString)
            .collect(Collectors.toList());
      }

      // Build ToolParameters with explicit required array
      ToolParameters toolParams = ToolParameters.builder()
          .type("object")
          .properties(propertiesMap)
          .required(requiredProperties) // Use explicit required array here
          .build();

      // Construct ToolSpecification with Mistral-compliant structure
      ToolSpecification toolSpecification = ToolSpecification.builder()
          .name(functionName)
          .description(functionDescription)
          .parameters(toolParams)
          .build();

      toolSpecifications.add(toolSpecification);
    }


    return toolSpecifications;
  }

  private static List<ToolSpecification> getTools(JSONArray tools, LangchainLLMConfiguration configuration) {

    List<ToolSpecification> toolSpecifications;
    if ("MISTRAL_AI".equals(configuration.getLlmType())) {
      toolSpecifications = getToolsMistral(tools, configuration);

    } else {
      toolSpecifications = getToolsGeneral(tools, configuration);

    }

    return toolSpecifications;

  }


  private static List<ToolSpecification> getToolsGeneral(JSONArray tools, LangchainLLMConfiguration configuration) {
    List<ToolSpecification> toolSpecifications = new ArrayList<>();

    // Iterate over each element in the tools JSONArray
    for (int i = 0; i < tools.length(); i++) {
      JSONObject functionEntry = tools.getJSONObject(i); // Get each JSONObject from the JSONArray
      JSONObject function = functionEntry.getJSONObject("function");

      String functionName = function.getString("name");
      String functionDescription = function.getString("description");

      JSONObject parameters = function.getJSONObject("parameters");
      JSONObject properties = parameters.getJSONObject("properties");

      Map<String, Map<String, Object>> propertiesMap = new HashMap<>();

      // Iterate over each property in the "properties" object
      for (String propertyName : properties.keySet()) {
        JSONObject propertyDetails = properties.getJSONObject(propertyName);

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("type", propertyDetails.getString("type"));
        propertyMap.put("description", propertyDetails.getString("description"));

        // Check for enum property and add if exists
        if (propertyDetails.has("enum")) {
          propertyMap.put("enum", propertyDetails.getJSONArray("enum").toList());
        }

        // Handle the "required" properties properly
        List<String> requiredProperties = new ArrayList<>();
        if (parameters.has("required")) {
          JSONArray requiredArray = parameters.getJSONArray("required");
          for (int j = 0; j < requiredArray.length(); j++) {
            requiredProperties.add(requiredArray.getString(j)); // Convert JSONArray to List<String>
          }
        }

        boolean isRequired = requiredProperties.contains(propertyName);
        propertyMap.put("required", isRequired);

        propertiesMap.put(propertyName, propertyMap);
      }

      // Create ToolParameters using the properties map
      ToolParameters toolParams = ToolParameters.builder()
          .properties(propertiesMap)
          .build();

      // Create ToolSpecification
      ToolSpecification toolSpecification = ToolSpecification.builder()
          .name(functionName)
          .description(functionDescription)
          .parameters(toolParams)
          .build();

      // Add the ToolSpecification to the list
      toolSpecifications.add(toolSpecification);
    }

    // Return the list of ToolSpecifications
    return toolSpecifications;
  }


}


