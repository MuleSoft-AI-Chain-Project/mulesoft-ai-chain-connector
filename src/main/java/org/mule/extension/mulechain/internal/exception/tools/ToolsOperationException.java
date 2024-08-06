package org.mule.extension.mulechain.internal.exception.tools;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class ToolsOperationException extends ModuleException {

  public ToolsOperationException(String message, Exception exception) {
    super(message, MuleChainErrorType.TOOLS_OPERATION_FAILURE, exception);
  }
}
