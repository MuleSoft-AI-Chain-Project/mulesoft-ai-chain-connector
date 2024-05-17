package org.mule.extension.langchain.internal;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.extension.langchain.internal.embedding.models.LangchainEmbeddingModelConfiguration;
import org.mule.extension.langchain.internal.llm.LangchainLLMConfiguration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "langchain")
@Extension(name = "Langchain")
@Configurations({LangchainLLMConfiguration.class, LangchainEmbeddingModelConfiguration.class})
public class LangchainExtension {

}
