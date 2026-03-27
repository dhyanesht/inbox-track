package com.dino.inbox_track.prompt;

import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LLMMessageParser {

  private final ObjectMapper mapper;

  public LLMMessageParser(ObjectMapper objectMapper) {
    this.mapper = objectMapper;
  }

  public List<EmailApplicationResponse> parseEmailApplicationResponse(String json) {
    try {
      // Clean common LLM formatting (e.g., markdown code blocks)
      String cleanJson = cleanJsonResponse(json);
      JsonNode node = mapper.readTree(cleanJson);

      // If the LLM returned a single object instead of an array, wrap it in a list
      if (node.isObject()) {
        EmailApplicationResponse singleResponse = mapper.treeToValue(node, EmailApplicationResponse.class);
        return List.of(singleResponse);
      }

      // If it is an array, parse it directly as a List
      if (node.isArray()) {
        return mapper.readValue(cleanJson, new TypeReference<List<EmailApplicationResponse>>() {
        });
      }

      return List.of();
    } catch (Exception e) {
      log.warn("Failed to parse EmailApplicationResponse JSON: {} - {}", json, e.getMessage());
      return List.of();
    }
  }

  public List<EmailApplicationClassification> parseClassificationResponse(List<String> jsonResponses) {

    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return jsonResponses.stream().map(json -> {
          try {
            // Clean common LLM formatting
            String cleanJson = cleanJsonResponse(json);
            JsonNode node = mapper.readTree(cleanJson);

            // Handle both single object and array formats
            if (node.isArray() && node.size() > 0) {
              cleanJson = mapper.writeValueAsString(node.get(0));
            }

            return Optional.of(mapper.readValue(cleanJson, EmailApplicationClassification.class));
          } catch (Exception e) {
            log.warn("Failed to parse EmailApplicationClassification JSON: {} - {}", json, e.getMessage());
            return Optional.<EmailApplicationClassification>empty();
          }
        })
        // unwrap only present values
        .flatMap(Optional::stream).toList();
  }

  public String cleanJsonResponse(String response) {
    if (response == null) {
      return "{}";
    }

    // Remove common LLM markdown wrappers
    return response.replaceAll("(?s)^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
  }

}
