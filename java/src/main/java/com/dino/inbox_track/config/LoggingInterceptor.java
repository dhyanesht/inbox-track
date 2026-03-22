package com.dino.inbox_track.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    // --- LOG REQUEST ---
    System.out.println("=========================== REQUEST BEGIN ===========================");
    System.out.println("URI         : " + request.getURI());
    System.out.println("Method      : " + request.getMethod());
    System.out.println("Headers     : " + request.getHeaders());
    String requestBody = new String(body, StandardCharsets.UTF_8);
    System.out.println("Request Body:");
    System.out.println(prettyPrintJson(requestBody)); // <--- Pretty print here
    System.out.println("=========================== REQUEST END   ===========================");

    // --- EXECUTE THE CALL ---
    ClientHttpResponse response = execution.execute(request, body);

    // --- LOG RESPONSE (Manual Buffering) ---

    // 1. Read the response body into a byte array immediately
    byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

    // 2. Log the body
    System.out.println("=========================== RESPONSE BEGIN ==========================");
    System.out.println("Status Code  : " + response.getStatusCode());
    System.out.println("Status Text  : " + response.getStatusText());
    System.out.println("Headers      : " + response.getHeaders());
    String responseBodyString = new String(responseBody, StandardCharsets.UTF_8);
    System.out.println("Response Body:");
    System.out.println(prettyPrintJson(responseBodyString)); // <--- Pretty print here too
    System.out.println("=========================== RESPONSE END   ==========================");

    // 3. Return a new ClientHttpResponse wrapper that returns the buffered bytes
    // This allows the RestClient to read the body again for the actual return type
    return new ClientHttpResponse() {
      @Override
      public HttpStatusCode getStatusCode() throws IOException {
        return response.getStatusCode();
      }

      @Override
      public String getStatusText() throws IOException {
        return response.getStatusText();
      }

      @Override
      public void close() {
        response.close();
      }

      @Override
      public InputStream getBody() throws IOException {
        // Return a new stream from the buffered bytes
        return new ByteArrayInputStream(responseBody);
      }

      @Override
      public org.springframework.http.HttpHeaders getHeaders() {
        return response.getHeaders();
      }
    };
  }

  /**
   * Helper method to pretty print JSON strings. If the string is not valid JSON, it returns it as-is.
   */
  private String prettyPrintJson(String jsonString) {
    try {
      Object jsonObject = objectMapper.readValue(jsonString, Object.class);
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    } catch (JsonProcessingException e) {
      // If parsing fails, just return the raw string (e.g. if it's plain text)
      return jsonString;
    }
  }
}