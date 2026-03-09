package com.dino.inbox_track.dto;

public record OllamaGenerateRequest(
        String model,
        String prompt,
        Boolean stream
) {}
