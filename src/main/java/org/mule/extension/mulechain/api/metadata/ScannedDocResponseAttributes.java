package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ScannedDocResponseAttributes implements Serializable {

  private final ArrayList<DocResponseAttribute> scannedDocAttributes;

  private final HashMap<String, Object> additionalAttributes;

  public ScannedDocResponseAttributes(ArrayList<DocResponseAttribute> scannedDocAttributes,
                                      HashMap<String, Object> additionalAttributes) {
    this.scannedDocAttributes = scannedDocAttributes;
    this.additionalAttributes = additionalAttributes;
  }

  public HashMap<String, Object> getAdditionalAttributes() {
    return additionalAttributes;
  }

  public ArrayList<DocResponseAttribute> getScannedDocAttributes() {
    return scannedDocAttributes;
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

