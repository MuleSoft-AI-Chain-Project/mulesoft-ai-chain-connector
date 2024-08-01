/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenStreamMetadataResolver implements OutputTypeResolver<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenStreamMetadataResolver.class);

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, String key) {
    LOGGER.info(key);
    LOGGER.info(metadataContext.toString());
    return metadataContext.getTypeBuilder().stringType().build();
  }

  @Override
  public String getCategoryName() {
    return "LangchainLLMPayload";
  }
}
