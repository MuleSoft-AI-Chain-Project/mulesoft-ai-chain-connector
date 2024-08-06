package org.mule.extension.mulechain.internal.exception.image;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class ImageGenerationException extends ModuleException {

  public ImageGenerationException(String message, Exception exception) {
    super(message, MuleChainErrorType.IMAGE_GENERATION_FAILURE, exception);
  }
}
