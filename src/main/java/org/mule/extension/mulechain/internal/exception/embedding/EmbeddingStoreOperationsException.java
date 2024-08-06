package org.mule.extension.mulechain.internal.exception.embedding;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class EmbeddingStoreOperationsException extends ModuleException {

  public EmbeddingStoreOperationsException(String message, Exception exception) {
    super(message, MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, exception);
  }
}
