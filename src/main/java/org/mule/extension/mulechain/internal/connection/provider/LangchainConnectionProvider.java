/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.connection.provider;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.extension.mulechain.internal.connection.LangchainLLMConnection;
import org.mule.runtime.api.connection.CachedConnectionProvider;


/**
 * This class (as it's name implies) provides connection instances and the funcionality to disconnect and validate those
 * connections.
 * <p>
 * All connection related parameters (values required in order to create a connection) must be
 * declared in the connection providers.
 * <p>
 * This particular example is a {@link PoolingConnectionProvider} which declares that connections resolved by this provider
 * will be pooled and reused. There are other implementations like {@link CachedConnectionProvider} which lazily creates and
 * caches connections or simply {@link ConnectionProvider} if you want a new connection each time something requires one.
 */
public class LangchainConnectionProvider implements PoolingConnectionProvider<LangchainLLMConnection> {

  @Override
  public LangchainLLMConnection connect() throws ConnectionException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void disconnect(LangchainLLMConnection arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public ConnectionValidationResult validate(LangchainLLMConnection arg0) {
    // TODO Auto-generated method stub
    return null;
  }


}
