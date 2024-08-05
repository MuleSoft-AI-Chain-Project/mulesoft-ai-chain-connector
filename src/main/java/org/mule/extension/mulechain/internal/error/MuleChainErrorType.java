package org.mule.extension.mulechain.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum MuleChainErrorType implements ErrorTypeDefinition<MuleChainErrorType> {

  INVALID_AUTHENTICATION, IO_EXCEPTION, TIME_OUT, RATE_LIMIT_OR_QUOTA_EXCEEDED;
}
