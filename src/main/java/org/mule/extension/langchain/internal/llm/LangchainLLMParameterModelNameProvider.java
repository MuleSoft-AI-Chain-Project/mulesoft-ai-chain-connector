package org.mule.extension.langchain.internal.llm;

import java.util.Set;


import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class LangchainLLMParameterModelNameProvider implements ValueProvider {

	@Override
	public Set<Value> resolve() throws ValueResolvingException {
		// TODO Auto-generated method stub
		
		return ValueBuilder.getValuesFor("gpt-3.5-turbo","gpt-4","gpt-4-turbo","dall-e-3",
										"mistral-small-latest","mistral-medium-latest", "mistral-large-latest", 
										"mistral","phi3","orca-mini","llama2","codellama","tinyllama",
										"claude-3-haiku-20240307","claude-3-opus-20240229","claude-3-sonnet-20240229");
	}

}
