/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.tools;

import dev.langchain4j.agent.tool.Tool;
import org.mule.extension.mulechain.internal.util.ExcludeFromGeneratedCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@ExcludeFromGeneratedCoverage
public class RestApiTool implements Tool {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestApiTool.class);

  private final String apiEndpoint;
  private final String name;
  private final String description;

  public RestApiTool(String apiEndpoint, String name, String description) {
    this.apiEndpoint = apiEndpoint;
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Tool("Check inventory for MULETEST0")
  //@Tool(name=AnnotationHelper.TOOL_NAME, value=AnnotationHelper.TOOL_NAME)
  public String execute(String input) {
    try {
      // Construct the full URL with parameters
      StringBuilder urlBuilder = new StringBuilder(apiEndpoint);

      URL url = new URL(urlBuilder.toString());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      conn.setRequestProperty("Accept", "application/json");
      String payload = "{\n \"materialNo\": \"MULETEST0\"}";

      LOGGER.info("Using tools");
      LOGGER.info(payload);
      LOGGER.info("URL: {}", url);

      conn.setDoOutput(true);
      byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
      try (OutputStream os = conn.getOutputStream()) {
        os.write(inputBytes, 0, inputBytes.length);
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
        LOGGER.info("200");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        br.close();

        return sb.toString();
      } else {
        LOGGER.info("Response Code: {}", responseCode);
        return "Error: Received response code " + responseCode;
      }
    } catch (IOException e) {
      LOGGER.warn("Error while executing requests for tool: ", e);
      return "Error: " + e.getMessage();
    }
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String name() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] value() {
    // TODO Auto-generated method stub
    return new String[] {};
  }
}
