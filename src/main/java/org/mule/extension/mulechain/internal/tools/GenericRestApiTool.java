package org.mule.extension.mulechain.internal.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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

public class GenericRestApiTool implements Tool {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericRestApiTool.class);

  private final String apiEndpoint;
  //private final Map<String, String> defaultParams;
  private final String name;
  private final String description;

  public GenericRestApiTool(String apiEndpoint, String name, String description) {
    this.apiEndpoint = apiEndpoint;
    //this.defaultParams = defaultParams;
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  //@Tool("Executes GET and POST requests for API endpoints.")
  ///Users/amir.khan/Documents/workspaces/langchain-mule-extension-test/src/main/resources/tool.config.json
  //@Tool(name = "DefaultName", value = "DefaultDescription")
  @Tool("Execute POST requests for API endpoints.")
  public String execute(@P("Input contains the URL for this request") String input,
                        @P("The method for the API. Support only POST") String method,
                        @P("The authorization header value for the request") String authHeader,
                        @P("The payload for the API, doublequotes must be masked") String payload) {
    try {
      LOGGER.info(method);

      // Construct the full URL with parameters for GET request
      StringBuilder urlBuilder = new StringBuilder(apiEndpoint);

      LOGGER.info("URL {}", urlBuilder);
      LOGGER.info("input {}", input);
      LOGGER.info("Method {}", method);
      LOGGER.info("payload {}", payload);
      if (method == null) {
        method = "GET";
      }

      LOGGER.info("apiEndpoint-{}", apiEndpoint);
      URL url = new URL(urlBuilder.toString());

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();

      conn.setRequestMethod(method.toUpperCase());
      conn.setRequestProperty("Authorization", authHeader);
      conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      conn.setRequestProperty("Accept", "application/json");

      // If the request method is POST, send the payload
      if ("POST".equalsIgnoreCase(method) && payload != null && !payload.isEmpty()) {
        LOGGER.info("POST");
        conn.setDoOutput(true);
        byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
          os.write(inputBytes, 0, inputBytes.length);
        }
      }

      int responseCode = conn.getResponseCode();
      LOGGER.info("Response code: {}", responseCode);
      if (responseCode == 200) {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        br.close();

        LOGGER.info(sb.toString());
        return sb.toString();
      } else {
        LOGGER.info(String.valueOf(responseCode));
        return "Error: Received response code " + responseCode;
      }
    } catch (IOException e) {
      LOGGER.warn("Error while executing POST requests for tool: ", e);
      return "Error: " + e.getMessage();
    }
  }

  @Tool("Execute GET requests for API endpoints.")
  public String execute(@P("Input contains the URL for this request") String input,
                        @P("The authorization header value for the request") String authHeader) {
    // Default to GET method with no payload
    return execute(input, "GET", authHeader, null);
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
    return null;
  }
}
