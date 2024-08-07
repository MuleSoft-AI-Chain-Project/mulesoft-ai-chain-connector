/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.error.provider;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static org.mule.extension.mulechain.internal.error.MuleChainErrorType.AI_SERVICES_FAILURE;

public class AiServiceErrorTypeProvider implements ErrorTypeProvider {

  @SuppressWarnings("rawtypes")
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return unmodifiableSet(new HashSet<>(Collections.singletonList(AI_SERVICES_FAILURE)));
  }
}
