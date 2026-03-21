package com.dino.inbox_track.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class EmailParsingService {

    public String extractHeader(Message message, String headerName) {
        return message.getPayload()
                .getHeaders()
                .stream()
                .filter(h -> headerName.equals(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("N/A");
    }

    public String extractPlainText(MessagePart payload) {
        StringBuilder text = new StringBuilder();
        extractTextFromPart(payload, text);

        return text.toString()
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")  // drop CSS
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")// drop JS
                .replaceAll("<[^>]+>", " ")                      // strip HTML tags
                .replaceAll("\\s+", " ")
                .replaceAll(
                        // Matches ALL URLs and markdown links
                        "(?i)(https?://[^\\s<>\"]+)|(?i)\\[[^\\]]+\\]\\([^)]+\\)",
                        " "
                )// normalize spaces
                .trim();
    }

    private void extractTextFromPart(MessagePart part, StringBuilder text) {
        if (part == null) {
            return;
        }

        String mimeType = part.getMimeType();
        String data = part.getBody() != null ? part.getBody().getData() : null;

        // 1) Plain text
        if ("text/plain".equalsIgnoreCase(mimeType) && data != null) {
            text.append(decodeBase64Url(data)).append("\n");
            return;
        }

        // 2) HTML as fallback if no plain text is present
        if ("text/html".equalsIgnoreCase(mimeType) && data != null) {
            text.append(decodeBase64Url(data)).append("\n");
            return;
        }

        // 3) multipart/* → recurse into children
        if (mimeType != null && mimeType.toLowerCase().startsWith("multipart/") && part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                extractTextFromPart(child, text);
            }
            return;
        }

        // 4) Some providers put content directly in body without parts
        if (data != null && (mimeType == null || !mimeType.startsWith("multipart/"))) {
            text.append(decodeBase64Url(data)).append("\n");
        }

    }

    private String decodeBase64Url(String encoded) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}