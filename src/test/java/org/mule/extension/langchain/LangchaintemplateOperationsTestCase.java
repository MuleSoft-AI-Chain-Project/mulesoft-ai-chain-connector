/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.langchain;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.junit.Test;

public class LangchaintemplateOperationsTestCase extends MuleArtifactFunctionalTestCase {

  /**
   * Specifies the mule config xml with the flows that are going to be executed in the tests, this file lives in the test resources.
   */
  @Override
  protected String getConfigFile() {
    return "test-mule-config.xml";
  }

  @Test
  public void executeInvokeiOperation() throws Exception {
    //    String payloadValue = ((String) flowRunner("sayHiFlow").run()
    //                                      .getMessage()
    //                                      .getPayload()
    //                                      .getValue());
    //    assertThat(payloadValue, is("Hello Mariano Gonzalez!!!"));
  }

  @Test
  public void executePredictOperation() throws Exception {
    //    String payloadValue = ((String) flowRunner("retrieveInfoFlow")
    //                                      .run()
    //                                      .getMessage()
    //                                      .getPayload()
    //                                      .getValue());
    //    assertThat(payloadValue, is("Using Configuration [configId] with Connection id [aValue:100]"));
  }
}
