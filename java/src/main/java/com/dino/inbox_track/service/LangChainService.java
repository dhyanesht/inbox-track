package com.dino.inbox_track.service;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_5;

import com.dino.inbox_track.client.CustomLLMChatModel;
import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.prompt.MessageClassifierTemplate;
import com.dino.inbox_track.prompt.PromptSanitizer;
import com.dino.inbox_track.prompt.SubjectClassifierTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LangChainService {

    private final OpenAiTokenCountEstimator tokenCountEstimator;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final PromptSanitizer promptSanitizer;

    public LangChainService(CustomLLMChatModel chatModel, PromptSanitizer promptSanitizer) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
        this.tokenCountEstimator = new OpenAiTokenCountEstimator(GPT_5);
        this.promptSanitizer = promptSanitizer;
    }

    public List<EmailApplicationResponse> filterJobApplicationSubjects(List<EmailDTO> subjects) throws JsonProcessingException, InterruptedException {

        int maxAllowedContextLength = 6000;


        List<EmailApplicationResponse> results = new ArrayList<>();

        List<EmailDTO> batch = new ArrayList<>();
        int currentTokenCount = 0;

        for (EmailDTO subject : subjects) {
            // Estimate token count for this single subject
            String subjectJson = objectMapper.writeValueAsString(subject);
            int subjectTokenCount = tokenCountEstimator.estimateTokenCountInText(subjectJson);

            // If this subject by itself is too big, ignore it (don’t add to any batch)
            if (subjectTokenCount > maxAllowedContextLength) {
                log.info("Skipping subject (too big): {} , {} ", subject.getEmailId(), subject.getSubject());
                continue;
            }
            // If this subject by itself is too big, still add it (you can’t skip)
            if (currentTokenCount + subjectTokenCount > maxAllowedContextLength && !batch.isEmpty()) {
                // Send current batch
                results.addAll(processBatch(batch, objectMapper));
                batch.clear();
                currentTokenCount = 0;
            }

            batch.add(subject);
            currentTokenCount += subjectTokenCount;
        }

        // send the final batch
        if (!batch.isEmpty()) {
            results.addAll(processBatch(batch, objectMapper));
        }

        return results;
    }

    private List<EmailApplicationResponse> processBatch(List<EmailDTO> batch, ObjectMapper objectMapper)
        throws JsonProcessingException {

        SubjectClassifierTemplate.SubjectClassifierPrompt promptTemplate = new SubjectClassifierTemplate.SubjectClassifierPrompt(batch);
        Prompt prompt = StructuredPromptProcessor.toPrompt(promptTemplate);
        String promptText = prompt.text();
        promptText = promptSanitizer.sanitize(promptText);
        String response = chatModel.chat(promptText);
        return objectMapper.readValue(response, new TypeReference<List<EmailApplicationResponse>>() {
        });
    }


    public String processEmailApplication(EmailDTO emailApp) throws InterruptedException {

        var message = trimToTokenLimitSmart(emailApp.getMessage(), 5000, tokenCountEstimator);
        emailApp.setMessage(message);
        MessageClassifierTemplate.MessageClassifierPrompt promptTemplate = new MessageClassifierTemplate.MessageClassifierPrompt(emailApp);
        Prompt prompt = StructuredPromptProcessor.toPrompt(promptTemplate);
        String promptText = prompt.text();
        promptText = promptSanitizer.sanitize(promptText);

        // Optionally log token count per batch
        ChatRequest chatRequest = ChatRequest.builder().messages(new ChatMessage[]{UserMessage.from(promptText)}).build();
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        return chatResponse.aiMessage().text();

    }

    public String trimToTokenLimitSmart(String text, int maxTokens, TokenCountEstimator estimator) {
        String result = trimToTokenLimit(text, maxTokens, estimator);

        // Try to preserve complete sentences/paragraphs
        String[] sentences = result.split("[.!?\\n]");
        StringBuilder smartTrim = new StringBuilder();

        for (String sentence : sentences) {
            String candidate = smartTrim + sentence + ".";
            if (estimator.estimateTokenCountInText(candidate) <= maxTokens) {
                smartTrim.append(sentence).append(". ");
            } else {
                break;
            }
        }

        return smartTrim.toString().trim();
    }

    public String trimToTokenLimit(String text, int maxTokens, TokenCountEstimator estimator) {
        int tokenCount = estimator.estimateTokenCountInText(text);
        if (tokenCount <= maxTokens) return text;

        int left = 0;
        int right = text.length();
        while (left < right) {
            int mid = left + (right - left) / 2;
            String truncated = text.substring(0, mid);
            int midTokens = estimator.estimateTokenCountInText(truncated);

            if (midTokens <= maxTokens) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        String result = text.substring(0, left);
        int finalTokens = estimator.estimateTokenCountInText(result);
        log.info("Trimmed from {} to {} tokens", tokenCount, finalTokens);
        return result;
    }


}
