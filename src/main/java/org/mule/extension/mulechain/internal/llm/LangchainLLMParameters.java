package org.mule.extension.mulechain.internal.llm;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class LangchainLLMParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(LangchainLLMParameterModelNameProvider.class)
  @Optional(defaultValue = "gpt-3.5-turbo")
  private String modelName;

  public String getModelName() {
    return modelName;
  }


  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "500")
  private Integer maxToken;

  public Integer getMaxToken() {
    return maxToken;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "0.7")
  private Double temperature;

  public Double getTemperature() {
    return temperature;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "60")
  private Integer timeoutInSeconds;

  public Integer getTimeoutInSeconds() {
    return timeoutInSeconds;
  }


}
