package com.dino.inbox_track.prompt;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptSanitizer {

    private final List<String> sensitiveValues;

    public PromptSanitizer(@Value("${app.sensitive.values:}") String values) {
        this.sensitiveValues = Arrays.stream(values.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList();
    }


    public String sanitize(String input) {
        return sensitiveValues.stream()
            .reduce(input, (str, word) ->
                    str.replaceAll("(?i)" + Pattern.quote(word), ""),
                (a, b) -> a);
    }
}
