package com.dino.inbox_track.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class OpenRouterClient {

    private final WebClient webClient;

    public OpenRouterClient(@Value("${openrouter.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Mono<String> chatCompletion(List<Map<String, String>> messages, String model) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class) // get JSON as Map
                .map(resp -> {
                    // extract the first assistant message
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    return message.get("content");
                });
    }
}