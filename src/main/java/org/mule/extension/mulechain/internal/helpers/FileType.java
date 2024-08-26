/**
 * (c) 2003-2024 MuleSoft, Inc. The software in this package is published under the terms of the Commercial Free Software license V.1 a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.extension.mulechain.internal.helpers;

import java.util.Arrays;

public enum FileType {

  ANY("any"), TEXT("text"), URL("url");

  private final String value;

  FileType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static FileType fromValue(String value) {
    return Arrays.stream(FileType.values())
        .filter(fileType -> fileType.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported File Type: " + value));
  }

}
