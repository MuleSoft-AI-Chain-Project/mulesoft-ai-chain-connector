package org.mule.extension.mulechain.internal.llm;


import org.mule.extension.mulechain.internal.embedding.stores.LangchainEmbeddingStoresOperations;
import org.mule.extension.mulechain.internal.image.models.LangchainImageModelsOperations;
import org.mule.extension.mulechain.internal.tools.LangchainToolsOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name="llm-configuration") 
@Operations({LangchainLLMOperations.class, LangchainEmbeddingStoresOperations.class, LangchainImageModelsOperations.class,LangchainToolsOperations.class})
//@ConnectionProviders(LangchainLLMConnectionProvider.class)
public class LangchainLLMConfiguration {

  @Parameter
  @OfValues(LangchainLLMTypeProvider.class)
  private String llmType;
  
  @Parameter
  @OfValues(LangchainLLMConfigType.class)
  private String configType;

  @Parameter
  private String filePath;

  public String getLlmType(){
    return llmType;
  }
  
  public String getConfigType() {
	   return configType;
  }

  public String getFilePath(){
    return filePath;
  }


}
