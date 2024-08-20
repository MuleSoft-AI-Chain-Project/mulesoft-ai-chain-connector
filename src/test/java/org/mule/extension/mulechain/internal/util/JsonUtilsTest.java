/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
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
    Assert.assertEquals("String are not equal", "{\"employee\":{\"name\":\"John Doe\",\"salary\":56000,\"married\":true}}",
                        object.toString());
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
