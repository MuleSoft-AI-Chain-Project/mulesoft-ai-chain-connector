package org.mule.extension.mulechain.internal.helpers;

import dev.langchain4j.model.output.Response;
import org.mule.extension.mulechain.api.metadata.LLMResponseAttributes;
import org.mule.extension.mulechain.api.metadata.ScannedDocResponseAttributes;
import org.mule.extension.mulechain.api.metadata.TokenUsage;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toInputStream;

public final class ResponseHelper {

  private ResponseHelper() {}

  public static Result<InputStream, LLMResponseAttributes> createLLMResponse(String response,
                                                                             dev.langchain4j.service.Result<?> result,
                                                                             Map<String, String> responseAttributes) {

    TokenUsage tokenUsage = result.tokenUsage() != null ? new TokenUsage(result.tokenUsage().inputTokenCount(),
                                                                         result.tokenUsage().outputTokenCount(),
                                                                         result.tokenUsage().totalTokenCount())
        : null;

    return createLLMResponse(response, tokenUsage, responseAttributes);
  }

  public static Result<InputStream, LLMResponseAttributes> createLLMResponse(String response,
                                                                             Response<?> result,
                                                                             Map<String, String> responseAttributes) {
    TokenUsage tokenUsage = result.tokenUsage() != null ? new TokenUsage(result.tokenUsage().inputTokenCount(),
                                                                         result.tokenUsage().outputTokenCount(),
                                                                         result.tokenUsage().totalTokenCount())
        : null;
    return createLLMResponse(response, tokenUsage, responseAttributes);
  }

  public static Result<InputStream, LLMResponseAttributes> createLLMResponse(String response,
                                                                             TokenUsage tokenUsage,
                                                                             Map<String, String> responseAttributes) {

    return Result.<InputStream, LLMResponseAttributes>builder()
        .attributes(new LLMResponseAttributes(tokenUsage, (HashMap<String, String>) responseAttributes))
        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
        .build();
  }

  public static Result<InputStream, ScannedDocResponseAttributes> createLLMResponse(String response,
                                                                                    List<ScannedDocResponseAttributes.DocResponseAttribute> docResponseAttributes,
                                                                                    Map<String, String> responseAttributes) {
    return Result.<InputStream, ScannedDocResponseAttributes>builder()
        .attributes(new ScannedDocResponseAttributes((ArrayList<ScannedDocResponseAttributes.DocResponseAttribute>) docResponseAttributes,
                                                     (HashMap<String, String>) responseAttributes))
        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
        .build();
  }

  public static Result<InputStream, Map<String, Object>> createLLMResponse(String response,
                                                                           Map<String, Object> responseAttributes) {
    return Result.<InputStream, Map<String, Object>>builder()
        .attributes(responseAttributes)
        .attributesMediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA)
        .output(toInputStream(response, StandardCharsets.UTF_8))
        .mediaType(org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON)
        .build();
  }
}
