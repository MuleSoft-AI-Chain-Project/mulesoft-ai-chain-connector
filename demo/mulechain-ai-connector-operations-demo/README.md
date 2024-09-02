MuleSoft AI Chain Connector Demo
====================================
Anypoint Studio demo for MuleSoft AI Chain Connector.


Prerequisites
---------------

* Anypoint Studio 7 with Mule ESB 4.3 Runtime.
* MuleChain AI Connector v1.0.0


How to Run Sample
-----------------

1. Import the project folder demo in Studio.
2. Change the API keys as per convenience in "envVars.json" file under src/main/resources folder.
3. Save the configuration & run the application


About the Sample
----------------

You can use postman to trigger curls under the web server http://localhost:8081

## Endpoints

* POST - /agent (Prompt template)
* POST - /chat (Chat answer prompt)
* POST - /chatMemory (Chat answer prompt with memory)
* POST - /embeddingNew (Create new embedding store)
* POST - /embeddingAdd (Add doc/file to the embedding store)
* POST - /embeddingAddFolder (Add folder to the embedding store)
* POST - /queryStore (Query the store directly)
* POST - /embeddingInfo (Get the formatted LLM response for embedding information)
* POST - /embeddingInfoLegacy (Legacy response for embedding information)
* POST - /image (Image generation)
* POST - /imageRead (Read the image URL and answer query)
* POST - /scanned (Perform operations on scanned docs)
* POST - /rag (Perform RAG over a file/doc provided)
* POST - /sentiments (Analyse sentiments)
* POST - /aiservice (Tools implementation of Langchain with AiServices)
* POST - /toolsMemory (Tools implementation with memory)
* POST - /chains (Tools implementation of Langchain with legacy chains)

_Please refer to [MuleSoft_AI_Chain_Connector_Postman_Collection](000_mulechain-ai-connector.postman_collection.json) available in source of this demo. You may have to provide own paths for various files._