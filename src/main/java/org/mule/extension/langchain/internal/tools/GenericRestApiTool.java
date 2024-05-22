package org.mule.extension.langchain.internal.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class GenericRestApiTool implements Tool {

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
    public String execute(@P("Input contains the URL for this request")String input, 
    		@P("The method for the API. Support only POST")String method, 
    		@P("The payload for the API, doublequotes must be masked")String payload) {
        try {
       	 	System.out.println(method);
        	
            // Construct the full URL with parameters for GET request
            StringBuilder urlBuilder = new StringBuilder(apiEndpoint);

       	 	System.out.println("URL " + urlBuilder.toString());
       	 	System.out.println("input " + input);
       	 	System.out.println("Method " + method);
       	 	System.out.println("payload " + payload);
	       	if (method == null) {
	             method="GET";
	        }

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method.toUpperCase());
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");    	

            // If the request method is POST, send the payload
            if ("POST".equalsIgnoreCase(method) && payload != null && !payload.isEmpty()) {
           	 	System.out.println("POST");
                conn.setDoOutput(true);
                byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(inputBytes, 0, inputBytes.length);
                }
            }

            int responseCode = conn.getResponseCode();
       	 	System.out.println(responseCode);
            if (responseCode == 200) {
     			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    	        StringBuilder sb = new StringBuilder();
    	        String line;
    	        while ((line = br.readLine()) != null) {
    	            sb.append(line+"\n");
    	        }
    	        br.close();

           	 	System.out.println(sb.toString());
    			return sb.toString();
            } else {
           	 	System.out.println(responseCode);
                return "Error: Received response code " + responseCode;
            }
        } catch (IOException e) {
       	 	System.out.println(e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Execute GET requests for API endpoints.")
    public String execute(@P("Input contains the URL for this request")String input) {
        // Default to GET method with no payload
        return execute(input, "GET", null);
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