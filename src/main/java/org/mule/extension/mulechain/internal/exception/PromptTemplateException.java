package org.mule.extension.mulechain.internal.exception;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class PromptTemplateException extends ModuleException {

  public PromptTemplateException(String message, Exception exception) {
    super(message, MuleChainErrorType.AI_SERVICES_FAILURE, exception);
  }
}
