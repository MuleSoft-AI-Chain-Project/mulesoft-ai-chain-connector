package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;
import java.util.Map;

public class LLMResponseAttributes implements Serializable {

  private final TokenUsage tokenUsage;
  private final Map<String, Object> attributes;

  public LLMResponseAttributes(TokenUsage tokenUsage, Map<String, Object> attributes) {
    this.tokenUsage = tokenUsage;
    this.attributes = attributes;
  }

  public TokenUsage getTokenUsage() {
    return tokenUsage;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
