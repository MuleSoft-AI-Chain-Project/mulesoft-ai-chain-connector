package org.mule.extension.langchain.internal.llm;


import org.mule.extension.langchain.internal.embedding.stores.LangchainEmbeddingStoresOperations;
import org.mule.extension.langchain.internal.image.models.LangchainImageModelsOperations;
import org.mule.extension.langchain.internal.streaming.LangchainLLMStreamingOperations;
import org.mule.extension.langchain.internal.tools.LangchainToolsOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.connectivity.*;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name="llm-configuration") 
@Operations({LangchainLLMOperations.class, LangchainEmbeddingStoresOperations.class, LangchainImageModelsOperations.class,LangchainToolsOperations.class, LangchainLLMStreamingOperations.class})
//@ConnectionProviders(LangchainLLMConnectionProvider.class)
public class LangchainLLMConfiguration {

  @Parameter
  @OfValues(LangchainLLMTypeProvider.class)
  private String llmType;
  
  @Parameter
  private String llmApiKey;

  public String getLlmType(){
    return llmType;
  }
  
  public String getLlmApiKey() {
	  return llmApiKey;
  }
}
