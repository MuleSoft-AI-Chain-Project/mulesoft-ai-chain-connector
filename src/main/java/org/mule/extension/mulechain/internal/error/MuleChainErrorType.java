/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum MuleChainErrorType implements ErrorTypeDefinition<MuleChainErrorType> {

  AI_SERVICES_FAILURE, IMAGE_ANALYSIS_FAILURE, IMAGE_GENERATION_FAILURE, IMAGE_PROCESSING_FAILURE, FILE_HANDLING_FAILURE, RAG_FAILURE, EMBEDDING_OPERATIONS_FAILURE, TOOLS_OPERATION_FAILURE, VALIDATION_FAILURE, STREAMING_FAILURE
}
