package org.mule.extension.mulechain.internal.helpers;


import java.util.Arrays;
import java.util.Set;

import org.mule.extension.mulechain.internal.llm.FileType;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class FileTypeEmbedding implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    // TODO Auto-generated method stub
    return ValueBuilder.getValuesFor(Arrays.stream(FileType.values()).map(FileType::name));
  }

}
