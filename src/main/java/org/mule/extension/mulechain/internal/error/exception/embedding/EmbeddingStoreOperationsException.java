/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.error.exception.embedding;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class EmbeddingStoreOperationsException extends ModuleException {

  public EmbeddingStoreOperationsException(String message, Exception exception) {
    super(message, MuleChainErrorType.EMBEDDING_OPERATIONS_FAILURE, exception);
  }
}
