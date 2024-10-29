package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LLMResponseAttributes implements Serializable {

  private final TokenUsage tokenUsage;
  private final HashMap<String, String> additionalAttributes;

  public LLMResponseAttributes(TokenUsage tokenUsage, HashMap<String, String> additionalAttributes) {
    this.tokenUsage = tokenUsage;
    this.additionalAttributes = additionalAttributes;
  }

  public TokenUsage getTokenUsage() {
    return tokenUsage;
  }

  public Map<String, String> getAdditionalAttributes() {
    return additionalAttributes;
  }
}
