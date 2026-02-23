package com.dino.inbox_track.dto;

public record OllamaGenerateResponse(
        String model,
        String response,
        Boolean done
        // add other fields from Ollama response if you need them
) {}
