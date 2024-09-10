# MuleChain AI Connector Reference

The MuleSoft AI Chain connector supports 15 operations, categorized into different topics for ease of use. Here's a structured overview of these operations:

## Agent Operations

### Agent Define Prompt Template

The `Agent define prompt template` operation is essential for using specific prompt templates with your LLMs. This operation allows you to define and compose AI functions using plain text to enable the creation of natural language prompts, generating responses, extracting information, invoking other prompts, or performing any text-based task.

#### Input configuration

**Module Configuration**

This refers to the MuleSoft AI Chain LLM Configuration set up in the Getting Started section.

**General Operation Fields**

- **Template**: Contains the prompt template for the operation.
- **Instructions**: Provides instructions for the LLM and outlines the goals of the task.
- **Dataset**: Specifies the dataset to be evaluated by the LLM using the provided template and instructions.

#### XML Configuration

The following is the XML configuration for this operation:

````xml
<ms-chainai:agent-define-prompt-template 
  doc:name="Agent define prompt template" 
  doc:id="f1c29c39-eac9-468c-9c46-4109a66303ec" 
  config-ref="MuleChain_AI_Llm_configuration" 
  template="#[payload.template]" 
  instructions="#[payload.instructions]" 
  dataset="#[payload.dataset]"
/>
````

#### Output Configuration 

**Payload**

This operation responds with a JSON payload containing the main LLM response. Additionally, attributes such as token usage are included as part of the metadata (attributes) but not within the main payload.

The following is an example payload:
````json
{
  "response": "{\n  \"type\": \"positive\",\n  \"response\": \"Thank you for your positive feedback on the training last week. We are glad to hear that you had a great experience. Have a nice day!\"\n}"
}
````

**Attributes**

Along with the JSON payload, this operation also returns attributes, which include information about token usage:

````json
{
  "tokenUsage": {
    "outputCount": 48,
    "totalCount": 85,
    "inputCount": 37
  }
}
````

- `tokenUsage`: The token usage metadata, returned as attributes.
- `outputCount`: The number of tokens used to generate the output.
- `totalCount`: The total number of tokens used for input and output.
- `inputCount`: The number of tokens used to process the input.

## Use Cases

Prompt templates can be applied in various scenarios, such as:

- Customer Service Agents: Enhance customer service by providing case summaries, case classifications, summarizing large datasets, and more.
- Sales Operation Agents: Aid sales teams in writing sales emails, summarizing cases for specific accounts, assessing the probability of closing deals, and more.
- Marketing Agents: Assist marketing teams in generating product descriptions, creating newsletters, planning social media campaigns, and more.