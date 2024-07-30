package org.mule.extension.mulechain.internal.streaming;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenStreamOutputResolver implements OutputTypeResolver<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenStreamOutputResolver.class);

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
