package com.dino.inbox_track.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gmail")
@Validated
public record GmailProperties(
    @NotBlank String credentialsPath,
    @NotBlank String tokensDirectory,
    @NotBlank String applicationName
) {

}