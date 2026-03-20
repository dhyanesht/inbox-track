package com.dino.inbox_track.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "groq")
@Validated
@Getter
@Setter
public class GroqLangChainConfig {
    @NotBlank
    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String modelName = "this will not work because of something. Need to check why";

    @Bean
    public ChatModel groqModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
