package org.mule.extension.langchain.internal.tools;

import dev.langchain4j.agent.tool.*;

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

public class RestApiTool implements Tool {

    private final String apiEndpoint;
    private final String name;
    private final String description;
    
    public RestApiTool(String apiEndpoint, String name, String description) {
        this.apiEndpoint = apiEndpoint;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Tool("Check inventory for MULETEST0")
    public String execute(String input) {
        try {
            // Construct the full URL with parameters
            StringBuilder urlBuilder = new StringBuilder(apiEndpoint);
            //urlBuilder.append(input);

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");    	
            String payload = "{\n \"materialNo\": \"MULETEST0\"}";
            
            System.out.println("Using tools");
            System.out.println(payload);
            System.out.println(url);

            conn.setDoOutput(true);
            byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(inputBytes, 0, inputBytes.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
            	 System.out.println("200");

     			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    	        StringBuilder sb = new StringBuilder();
    	        String line;
    	        while ((line = br.readLine()) != null) {
    	            sb.append(line+"\n");
    	        }
    	        br.close();

    			return sb.toString();
            	 
//                Scanner scanner = new Scanner(url.openStream());
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
