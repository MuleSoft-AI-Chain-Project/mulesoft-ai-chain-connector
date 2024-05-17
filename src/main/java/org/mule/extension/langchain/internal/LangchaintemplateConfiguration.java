package org.mule.extension.langchain.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(LangchaintemplateOperations.class)
@ConnectionProviders(LangchaintemplateConnectionProvider.class)
public class LangchaintemplateConfiguration {

  @Parameter
  @OfValues(LLMProvider.class)
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
