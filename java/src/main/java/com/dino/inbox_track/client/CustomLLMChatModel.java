package com.dino.inbox_track.client;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component("customLLMChatModel")
public class CustomLLMChatModel implements ChatModel {

  private final GenericLLMClient client;

  public CustomLLMChatModel(GenericLLMClient client) {
    this.client = client;
  }

  @Override
  public ChatResponse doChat(ChatRequest chatRequest) {

    // Convert LangChain4J ChatMessages to a format your LLM API understands
    List<Map<String, String>> messages = chatRequest.messages().stream()
        .map(msg -> {
          String role = mapRole(msg);
          String content;

          if (msg instanceof UserMessage userMsg) {
            content = userMsg.contents().stream()
                .filter(TextContent.class::isInstance)       // keep only TextContent
                .map(c -> ((TextContent) c).text())        // cast safely
                .collect(Collectors.joining(" "));

          } else if (msg instanceof AiMessage aiMsg) {
            content = aiMsg.text();
          } else {
            content = msg.toString(); // fallback for SystemMessage or unknown
          }

          return Map.of(
              "role", role,
              "content", content
          );
        })
        .toList();

    try {
      // Call your GenericLLMClient
      String aiText = client.chatCompletion(messages);

      // Wrap response as ChatResponse
      AiMessage aiMessage = AiMessage.from(aiText);

      return ChatResponse.builder()
          .aiMessage(aiMessage)
          .build();

    } catch (Exception e) {
      throw new RuntimeException("LLM request failed", e);
    }
  }

  private String mapRole(ChatMessage msg) {
    // Map LangChain4J message roles to LLM roles (user/assistant/system)
    if (msg instanceof UserMessage) {
      return "user";
    }
    if (msg instanceof SystemMessage) {
      return "system";
    }
    // You can extend for AIMessage etc.
    return "user";
  }

  @Override
  public ChatRequestParameters defaultRequestParameters() {
    return ChatRequestParameters.builder().build(); // empty/default
  }

  @Override
  public ModelProvider provider() {
    return ModelProvider.OTHER;
  }
}