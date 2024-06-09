package org.mule.extension.langchain.internal.streaming;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class TokenStreamOutputResolver implements OutputTypeResolver<String> {
    @Override
    public MetadataType getOutputType(MetadataContext metadataContext, String key) {
        System.out.println(key);
        System.out.println(metadataContext.toString());
        return metadataContext.getTypeBuilder().stringType().build();
    }

    @Override
    public String getCategoryName() {
        return "LangchainLLMPayload";
    }
}
