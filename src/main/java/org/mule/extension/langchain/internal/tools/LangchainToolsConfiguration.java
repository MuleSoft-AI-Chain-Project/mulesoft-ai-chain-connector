package org.mule.extension.langchain.internal.tools;



import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name="anypoint-configuration") 
@Operations({})
//@ConnectionProviders(LangchainLLMConnectionProvider.class)
public class LangchainToolsConfiguration {

  @Parameter
  private String anypointUrl;
  
  @Parameter
  private String anypointClientId;

  @Parameter
  private String anypointClientSecret;

  public String getAnypointUrl(){
    return anypointUrl;
  }
  
  public String getAnypointClientId() {
	  return anypointClientId;
  }

  public String getAnypointClientSecret() {
	  return anypointClientSecret;
  }

}
