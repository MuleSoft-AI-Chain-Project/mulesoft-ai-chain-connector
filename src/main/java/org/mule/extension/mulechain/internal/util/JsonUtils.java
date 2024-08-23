/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.util;

import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.Result;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class JsonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

  private JsonUtils() {}

  public static JSONObject readConfigFile(String filePath) {
    Path path = Paths.get(filePath);
    if (Files.exists(path)) {
      try {
        String content = new String(Files.readAllBytes(path));
        return new JSONObject(content);
      } catch (Exception e) {
        LOGGER.error("Unable to read the config file: " + filePath, e);
      }
    } else {
      LOGGER.warn("File does not exist: {}", filePath);
    }
    return null;
  }

  public static JSONObject getTokenUsage(Result<?> results) {
    JSONObject tokenUsage = new JSONObject();
    tokenUsage.put(MuleChainConstants.INPUT_COUNT, results.tokenUsage().inputTokenCount());
    tokenUsage.put(MuleChainConstants.OUTPUT_COUNT, results.tokenUsage().outputTokenCount());
    tokenUsage.put(MuleChainConstants.TOTAL_COUNT, results.tokenUsage().totalTokenCount());
    return tokenUsage;
  }
}
