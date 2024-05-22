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
    @Tool("Execute GET and POST requests for API endpoints.")
    public String execute(@P("Input contains all information about the API such as name and description.")String input, 
    		@P("The method for the API. Support only GET or POST")String method, 
    		@P("The payload for the API")String payload) {
        try {
            // Construct the full URL with parameters for GET request
            StringBuilder urlBuilder = new StringBuilder(apiEndpoint);
            if ("GET".equalsIgnoreCase(method)) {
//                urlBuilder.append(input);
//                for (Map.Entry<String, String> param : defaultParams.entrySet()) {
//                    urlBuilder.append("&").append(param.getKey()).append("=").append(param.getValue());
//                }
            }
//
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method.toUpperCase());
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");    	

            // If the request method is POST, send the payload
            if ("POST".equalsIgnoreCase(method) && payload != null && !payload.isEmpty()) {
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
//                Scanner scanner = new Scanner(conn.getInputStream());
//                StringBuilder response = new StringBuilder();
//                while (scanner.hasNext()) {
//                    response.append(scanner.nextLine());
//                }
//                scanner.close();
//                return response.toString();
            } else {
           	 	System.out.println(responseCode);
                return "Error: Received response code " + responseCode;
            }
        } catch (IOException e) {
       	 	System.out.println(e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool("This method is used for GET, whenever there is no payload defined.")
    public String execute(String input) {
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