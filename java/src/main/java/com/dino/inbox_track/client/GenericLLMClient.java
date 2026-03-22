package com.dino.inbox_track.client;

import com.dino.inbox_track.config.LoggingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionChoice;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GenericLLMClient {

  private final ObjectMapper objectMapper;
  private final String modelName;
  private final RestClient restClient;

  public GenericLLMClient(@Value("${llm.base-url}") String baseUrl, @Value("${llm.api-key}") String apiKey,
      @Value("${llm.model-name}") String modelName, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.modelName = modelName;

    // 1. Define the Timeout configuration (5 seconds to connect, 60 seconds to read)
    RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(5))
        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
        .setResponseTimeout(Timeout.ofSeconds(240)) // This is the Read Timeout
        .build();

    // 2. Build the Apache HttpClient
    HttpClient httpClient = HttpClients.custom()
        .setDefaultRequestConfig(config)
        .build();

    // 3. Create the Spring Factory using the Apache client
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .requestInterceptor(new LoggingInterceptor())
        .build();
  }

  public String chatCompletion(List<Map<String, String>> messages) throws Exception {
    return chatCompletion(messages, this.modelName);
  }

  public String chatCompletion(List<Map<String, String>> messages, String model) throws Exception {

    Map<String, Object> requestBody = Map.of(
        "model", model,
        "messages", messages,
        "stream", false
    );
    ChatCompletionResponse completion = ChatCompletionResponse.builder().build();
    try {
      String responseJsonRc = restClient.post()
          .uri("/chat/completions")
          .body(requestBody)
          .retrieve()
          .body(String.class);
      Thread.sleep(Duration.ofSeconds(20).toMillis()); // rate limiting the API.
      completion = objectMapper.readValue(responseJsonRc, ChatCompletionResponse.class);
    } catch (ResourceAccessException e) {
      log.error("ResourceAccessException during LLM Call");
      log.error(e.getMessage());
      completion = ChatCompletionResponse.builder().build();
    } catch (RestClientException e) {
      e.printStackTrace();
    }

    // Deserialize JSON to LangChain4J ChatCompletionResponse
    // Return the content of the first assistant message
    if (completion.choices() != null && !completion.choices().isEmpty()) {
      ChatCompletionChoice firstChoice = completion.choices().get(0);
      if (firstChoice.message() != null) {
        return firstChoice.message().content();
      }
    }
    return null;
  }
}
