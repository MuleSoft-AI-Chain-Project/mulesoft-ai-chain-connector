package org.mule.extension.mulechain.internal.embedding.models;

import org.mule.extension.mulechain.internal.LangchainConnectionProvider;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name="embedding-model-configuration") 
@Operations(LangchainEmbeddingModelsOperations.class)
@ConnectionProviders(LangchainConnectionProvider.class)
public class LangchainEmbeddingModelConfiguration {

  @Parameter
  private String projectId;
  
  @Parameter
  private String modelName;

  public String getProjectId(){
    return projectId;
  }
  
  public String getModelName() {
	  return modelName;
  }
}
