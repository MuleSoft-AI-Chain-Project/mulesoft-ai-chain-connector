package org.mule.extension.mulechain.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum MuleChainErrorType implements ErrorTypeDefinition<MuleChainErrorType> {

  MODEL_CREATION_FAILED, AI_SERVICES_FAILURE, IMAGE_ANALYSIS_FAILURE, IMAGE_GENERATION_FAILURE, IMAGE_PROCESSING_FAILURE, FILE_HANDLING_FAILURE, RAG_FAILURE, EMBEDDING_OPERATIONS_FAILURE, TOOLS_OPERATION_FAILURE
}
