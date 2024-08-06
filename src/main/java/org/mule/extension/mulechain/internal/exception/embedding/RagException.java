package org.mule.extension.mulechain.internal.exception.embedding;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class RagException extends ModuleException {

  public RagException(String message, Exception exception) {
    super(message, MuleChainErrorType.RAG_FAILURE, exception);
  }
}
