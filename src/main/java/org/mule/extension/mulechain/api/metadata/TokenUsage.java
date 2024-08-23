package org.mule.extension.mulechain.api.metadata;

import java.io.Serializable;

public class TokenUsage implements Serializable {

  private final int inputCount;
  private final int outputCount;
  private final int totalCount;

  public TokenUsage(int inputCount, int outputCount, int totalCount) {
    this.inputCount = inputCount;
    this.outputCount = outputCount;
    this.totalCount = totalCount;
  }

  public int getInputCount() {
    return inputCount;
  }

  public int getOutputCount() {
    return outputCount;
  }

  public int getTotalCount() {
    return totalCount;
  }
}
