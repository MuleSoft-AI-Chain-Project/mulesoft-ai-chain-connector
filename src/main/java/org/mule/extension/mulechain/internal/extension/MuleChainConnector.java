/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.extension;

import org.mule.extension.mulechain.internal.error.MuleChainErrorType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.sdk.api.annotation.JavaVersionSupport;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "mulechain")
@Extension(name = "MuleChain AI", category = Category.CERTIFIED)
@Configurations({LangchainLLMConfiguration.class})
@RequiresEnterpriseLicense(allowEvaluationLicense = true)
@ErrorTypes(MuleChainErrorType.class)
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
public class MuleChainConnector {

}
