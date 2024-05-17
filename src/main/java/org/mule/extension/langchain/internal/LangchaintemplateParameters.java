package org.mule.extension.langchain.internal;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class LangchaintemplateParameters {
	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(ModelNameProvider.class)
	@Optional(defaultValue = "gpt-3.5-turbo")
	private String modelName;

	public String getModelName() {
		return modelName;
	}

}
