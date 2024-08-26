package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ScannedDocResponseAttributes implements Serializable {

  private final ArrayList<DocResponseAttribute> docResponseAttribute;

  private final HashMap<String, Object> attributes;

  public ScannedDocResponseAttributes(ArrayList<DocResponseAttribute> docResponseAttribute, HashMap<String, Object> attributes) {
    this.docResponseAttribute = docResponseAttribute;
    this.attributes = attributes;
  }

  public HashMap<String, Object> getAttributes() {
    return attributes;
  }

  public ArrayList<DocResponseAttribute> getDocResponseAttribute() {
    return docResponseAttribute;
  }

  public static class DocResponseAttribute implements Serializable {

    private final int page;
    private final TokenUsage tokenUsage;

    public DocResponseAttribute(int page, TokenUsage tokenUsage) {
      this.page = page;
      this.tokenUsage = tokenUsage;
    }

    public int getPage() {
      return page;
    }

    public TokenUsage getTokenUsage() {
      return tokenUsage;
    }
  }
}

