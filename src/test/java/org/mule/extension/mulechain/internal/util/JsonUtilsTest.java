package org.mule.extension.mulechain.internal.util;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonUtilsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtilsTest.class);


  @Before
  public void set_up() {
    LOGGER.info("Setting up JsonUtilsTest");
  }

  @Test
  public void testHappyPath() {
    Path jsonFile = Paths.get("src", "test", "resources", "sample-json.txt");
    JSONObject object = JsonUtils.readConfigFile(jsonFile.toFile().getAbsoluteFile().toString());
    Assert.assertNotNull("Returned object is null", object);
    Assert.assertEquals("String are not equal", object.toString(),
                        "{\"employee\":{\"name\":\"John Doe\",\"salary\":56000,\"married\":true}}");
  }

  @Test
  public void testFileNotExists() {
    Path jsonFile = Paths.get("src", "test", "resources", "invalid-file.txt");
    JSONObject object = JsonUtils.readConfigFile(jsonFile.toFile().getAbsoluteFile().toString());
    Assert.assertNull("Returned object is non-null but expecting null", object);
  }

  @Test
  public void testInvalidJson() {
    Path jsonFile = Paths.get("src", "test", "resources", "invalid-json.txt");
    JSONObject object = JsonUtils.readConfigFile(jsonFile.toFile().getAbsoluteFile().toString());
    Assert.assertNull("Returned object is non-null but expecting null", object);
  }

}
