package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LLMResponseAttributes implements Serializable {

  private final TokenUsage tokenUsage;
  private final HashMap<String, Object> additionalAttributes;

  public LLMResponseAttributes(TokenUsage tokenUsage, HashMap<String, Object> additionalAttributes) {
    this.tokenUsage = tokenUsage;
    this.additionalAttributes = additionalAttributes;
  }

  public TokenUsage getTokenUsage() {
    return tokenUsage;
  }

  public Map<String, Object> getAdditionalAttributes() {
    return additionalAttributes;
  }
}
