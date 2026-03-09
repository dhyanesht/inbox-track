package com.dino.inbox_track.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PromptSanitizer {

    private static List<String> bannedWords = List.of();

    public PromptSanitizer(
            @Value("${app.banned.words:}") String bannedWordsStr) {
        bannedWords = Optional.of(bannedWordsStr)
                .filter(str -> !str.isEmpty())
                .map(str -> Arrays.asList(str.split(",")))
                .orElse(List.of())
                .stream()
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }

    // TODO: Write Test. This is not working. Check it.
    public static String sanitizeNames(String prompt) {
        return bannedWords.stream()
                .map(Pattern::quote)
                .reduce(prompt, (str, escaped) -> str.replace(escaped, ""), (a, b) -> a);
    }
}
