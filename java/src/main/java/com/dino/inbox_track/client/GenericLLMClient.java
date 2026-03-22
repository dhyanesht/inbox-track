package com.dino.inbox_track.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionChoice;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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
      Thread.sleep(Duration.ofSeconds(10).toMillis()); // rate limiting the API.
      completion = objectMapper.readValue(responseJsonRc, ChatCompletionResponse.class);
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
