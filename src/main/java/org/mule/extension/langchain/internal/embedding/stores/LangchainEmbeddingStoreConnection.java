package org.mule.extension.langchain.internal.embedding.stores;


/**
 * This class represents an extension connection just as example (there is no real connection with anything here c:).
 */
public final class LangchainEmbeddingStoreConnection {

  private final String id;

  public LangchainEmbeddingStoreConnection(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void invalidate() {
    // do something to invalidate this connection!
  }
}
