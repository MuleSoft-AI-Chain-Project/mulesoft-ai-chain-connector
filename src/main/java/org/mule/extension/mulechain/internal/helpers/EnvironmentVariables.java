/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.helpers;

import java.io.IOException;
import java.util.Map;

public class EnvironmentVariables {

  private EnvironmentVariables() {}

  public static void setVar(String varNam, String varValue) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    Map<String, String> environment = processBuilder.environment();
    environment.put(varNam, varValue);

    // Start a new process with the modified environment
    processBuilder.command("bash", "-c", "echo $MY_ENV_VAR");
    processBuilder.inheritIO();
    processBuilder.start();
  }
}
