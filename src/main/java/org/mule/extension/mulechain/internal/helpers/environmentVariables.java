package org.mule.extension.mulechain.internal.helpers;

import java.io.IOException;
import java.util.Map;

public class environmentVariables {
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