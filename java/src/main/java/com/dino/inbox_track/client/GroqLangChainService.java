package com.dino.inbox_track.client;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class GroqLangChainService {

    private final ChatModel llm;

    public GroqLangChainService(ChatModel llm) {
        this.llm = llm;
    }

    public String classifyEmail(String prompt) {
        return llm.chat(prompt);
    }
}