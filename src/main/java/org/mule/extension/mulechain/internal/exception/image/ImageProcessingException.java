package org.mule.extension.mulechain.internal.exception.image;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class ImageProcessingException extends ModuleException {

  public ImageProcessingException(String message, Exception exception) {
    super(message, MuleChainErrorType.IMAGE_PROCESSING_FAILURE, exception);
  }
}
