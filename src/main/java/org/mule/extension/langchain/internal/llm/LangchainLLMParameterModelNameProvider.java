package org.mule.extension.langchain.internal.llm;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class LangchainLLMParameterModelNameProvider implements ValueProvider {

	private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
	"gpt-3.5-turbo",
	"gpt-4",
	"gpt-4-turbo",
	"dall-e-3",
	"mistral-small-latest",
	"mistral-medium-latest",
	"mistral-large-latest",
	"mistral",
	"phi3",
	"orca-mini",
	"llama2",
	"codellama",
	"tinyllama",
	"claude-3-haiku-20240307",
	"claude-3-opus-20240229",
	"claude-3-sonnet-20240229",
	"anthropic.claude-instant-v1",
	"anthropic.claude-v2",
	"anthropic.claude-v2:1",
	"anthropic.claude-3-sonnet-20240229-v1:0",
	"anthropic.claude-3-haiku-20240307-v1:0",
	"ai21.j2-mid-v1");

	@Override
	public Set<Value> resolve() throws ValueResolvingException {
		
		
		return VALUES_FOR;
	}

}