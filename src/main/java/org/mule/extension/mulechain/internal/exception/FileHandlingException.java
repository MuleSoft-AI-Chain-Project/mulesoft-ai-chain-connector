package org.mule.extension.mulechain.internal.exception;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class FileHandlingException extends ModuleException {

  public FileHandlingException(String message, Exception exception) {
    super(message, MuleChainErrorType.FILE_HANDLING_FAILURE, exception);
  }

  public FileHandlingException(String message) {
    super(message, MuleChainErrorType.FILE_HANDLING_FAILURE);
  }
}
