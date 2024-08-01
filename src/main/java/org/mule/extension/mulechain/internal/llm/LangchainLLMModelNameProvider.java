package org.mule.extension.mulechain.internal.llm;

import java.util.Set;

import org.mule.extension.mulechain.internal.llm.type.LangchainLLMType;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class LangchainLLMModelNameProvider implements ValueProvider {

  @Parameter
  private String llmType;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return ValueBuilder.getValuesFor(LangchainLLMType.valueOf(llmType).getModelNameStream());
  }

}
