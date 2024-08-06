/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.operation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.config.LangchainLLMConfiguration;
import org.mule.extension.mulechain.internal.constants.MuleChainConstants;
import org.mule.extension.mulechain.internal.llm.config.ConfigExtractor;
import org.mule.extension.mulechain.internal.util.JsonUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Config;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;


/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class LangchainImageModelsOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LangchainImageModelsOperations.class);

  /**
   * Reads an image from a URL.
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-read")
  public String readFromImage(@Config LangchainLLMConfiguration configuration, String data, String contextURL) {

    ChatLanguageModel model = configuration.getModel();

    UserMessage userMessage = UserMessage.from(
                                               TextContent.from(data),
                                               ImageContent.from(contextURL));

    Response<AiMessage> response = model.generate(userMessage);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, response.content().text());
    jsonObject.put(MuleChainConstants.TOKEN_USAGE, JsonUtils.getTokenUsage(response));

    return jsonObject.toString();
  }

  /**
   * Generates an image based on the prompt in data
   */
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-generate")
  public String drawImage(@Config LangchainLLMConfiguration configuration, String data) {
    ConfigExtractor configExtractor = configuration.getConfigExtractor();
    ImageModel model = OpenAiImageModel.builder()
        .modelName(configuration.getModelName())
        .apiKey(configExtractor.extractValue("OPENAI_API_KEY"))
        .build();

    Response<Image> response = model.generate(data);
    LOGGER.info("Generated Image: {}", response.content().url());

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(MuleChainConstants.RESPONSE, response.content().url());

    return jsonObject.toString();
  }

  /**
   * Reads an scanned document.
   */

  /*
  @MediaType(value = ANY, strict = false)
  @Alias("IMAGE-read-scanned-documents")
  public String readScannedDocumentPDF(@Config LangchainLLMConfiguration configuration, String data, String filePath) {
  
    ChatLanguageModel model = configuration.getModel();
  
    String sourceDir = filePath;
  
    JSONObject jsonObject = new JSONObject();
    JSONArray docPages = new JSONArray();
    
    try (PDDocument document = Loader.loadPDF(new File(sourceDir))) {
  
      PDFRenderer pdfRenderer = new PDFRenderer(document);
      int totalPages = document.getNumberOfPages();
      LOGGER.info("Total files to be converted -> " + totalPages);
      jsonObject.put(MuleChainConstants.TOTAL_PAGES, totalPages);
  
      JSONObject docPage;
      for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
  
        BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, 300);
        LOGGER.info("Reading page -> " + pageNumber);
  
        String imageBase64 = convertToBase64String(image);
        UserMessage userMessage = UserMessage.from(
                                                   TextContent.from(data),
                                                   ImageContent.from(imageBase64, "image/png"));
  
        Response<AiMessage> response = model.generate(userMessage);
  
        docPage = new JSONObject();
        docPage.put(MuleChainConstants.PAGE, pageNumber + 1);
        docPage.put(MuleChainConstants.RESPONSE, response.content().text());
        docPage.put(MuleChainConstants.TOKEN_USAGE, JsonUtils.getTokenUsage(response));
        docPages.put(docPage);
      }
  
    } catch (IOException e) {
      LOGGER.info("Error occurred while processing the file: " + e.getMessage());
    }
  
    jsonObject.put(MuleChainConstants.PAGES, docPages);
  
    return jsonObject.toString();
  }
  
  private String convertToBase64String(BufferedImage image) {
    String base64String;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", outputStream);
      byte[] imageBytes = outputStream.toByteArray();
      base64String = Base64.getEncoder().encodeToString(imageBytes);
      return base64String;
    } catch (IOException e) {
      e.printStackTrace();
      LOGGER.info("Error occurred while processing the file: " + e.getMessage());
      return "Error";
    }
  } */
}
