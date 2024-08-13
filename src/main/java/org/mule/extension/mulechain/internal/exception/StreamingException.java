/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.exception;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class StreamingException extends ModuleException {

  public StreamingException(String message, Throwable throwable) {
    super(message, MuleChainErrorType.STREAMING_FAILURE, throwable);
  }
}
