/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.config;



import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name = "anypoint-configuration")
@Operations({})
//@ConnectionProviders(LangchainLLMConnectionProvider.class)
public class LangchainToolsConfiguration {

  @Parameter
  private String anypointUrl;

  @Parameter
  private String anypointClientId;

  @Parameter
  private String anypointClientSecret;

  public String getAnypointUrl() {
    return anypointUrl;
  }

  public String getAnypointClientId() {
    return anypointClientId;
  }

  public String getAnypointClientSecret() {
    return anypointClientSecret;
  }

}
