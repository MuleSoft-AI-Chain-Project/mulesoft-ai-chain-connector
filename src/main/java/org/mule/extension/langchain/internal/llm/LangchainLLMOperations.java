package org.mule.extension.langchain.internal.llm;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;


import static java.time.Duration.ofSeconds;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.HashMap;
import java.util.Map;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import software.amazon.awssdk.regions.Region;
/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainLLMOperations {


	private ChatLanguageModel createModel(LangchainLLMConfiguration configuration, LangchainLLMParameters LangchainParams) {
	    ChatLanguageModel model = null;
	    switch (configuration.getLlmType()) {
	        case "OPENAI":
	            model = OpenAiChatModel.builder()
	                    //.apiKey(configuration.getLlmApiKey())
						.apiKey(System.getenv("OPENAI_API_KEY").replace("\n", "").replace("\r", ""))
	                    .modelName(LangchainParams.getModelName())
	                    .temperature(0.3)
	                    .timeout(ofSeconds(60))
	                    .logRequests(true)
	                    .logResponses(true)
	                    .build();
	            break;
	        case "MISTRAL_AI":
	            model = MistralAiChatModel.builder()
	                    //.apiKey(configuration.getLlmApiKey())
						.apiKey(System.getenv("MISTRAL_AI_API_KEY").replace("\n", "").replace("\r", ""))
	                    .modelName(LangchainParams.getModelName())
	                    .temperature(0.3)
	                    .timeout(ofSeconds(60))
	                    .logRequests(true)
	                    .logResponses(true)
	                    .build();
	            break;
	        case "OLLAMA":
	            model = OllamaChatModel.builder()
	                    //.baseUrl(configuration.getLlmApiKey())
						.baseUrl(System.getenv("OLLAMA_BASE_URL").replace("\n", "").replace("\r", ""))
	                    .modelName(LangchainParams.getModelName())
	                    .build();
	            break;
	        case "ANTHROPIC":
	            model = AnthropicChatModel.builder()
	                    //.apiKey(configuration.getLlmApiKey())
						.apiKey(System.getenv("ANTHROPIC_API_KEY").replace("\n", "").replace("\r", ""))
	                    .modelName(LangchainParams.getModelName())
	                    .logRequests(true)
	                    .logResponses(true)
	                    .build();
	            break;
			case "AWS_BEDROCK":
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
			case "AZURE_OPENAI":
				//String[] creds = configuration.getLlmApiKey().split("mulechain"); 
				// For authentication, set the following environment variables:
        		// AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 				model = AzureOpenAiChatModel.builder()
                    .apiKey(System.getenv("AZURE_OPENAI_KEY").replace("\n", "").replace("\r", ""))
                    .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT").replace("\n", "").replace("\r", ""))
                    .deploymentName(System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME").replace("\n", "").replace("\r", ""))
                    .temperature(0.3)
                    .logRequestsAndResponses(true)
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
