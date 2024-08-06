package org.mule.extension.mulechain.internal.exception.image;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class ImageAnalyzerException extends ModuleException {

  public ImageAnalyzerException(String message, Exception exception) {
    super(message, MuleChainErrorType.IMAGE_ANALYSIS_FAILURE, exception);
  }
}
