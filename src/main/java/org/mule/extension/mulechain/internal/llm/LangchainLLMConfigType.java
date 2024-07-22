package org.mule.extension.mulechain.internal.llm;

import java.util.Set;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class LangchainLLMConfigType implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    // TODO Auto-generated method stub
    return ValueBuilder.getValuesFor("Environment Variables", "Configuration Json");
  }

}

