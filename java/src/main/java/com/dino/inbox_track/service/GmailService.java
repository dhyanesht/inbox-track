package com.dino.inbox_track.service;

import com.dino.inbox_track.client.GmailServiceFactory;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.dino.inbox_track.dto.EmailDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private static final String USER = "me";
    private final GmailServiceFactory gmailServiceFactory;
    private final EmailParsingService emailParsingService;
    private final EmailService emailService;
    private final JobService jobService;
    private final OllamaService ollamaService;
    private final LangChainService langChainService;


    public List<String> getLabelNames() throws Exception {
        var response = gmailService().users().labels().list("me").execute();
        return response.getLabels().stream().map(Label::getName).toList();

    }

    public List<EmailApplicationClassification> getRecentEmails(int daysBack) throws Exception {

        var service = gmailService();

        LocalDate endDate = LocalDate.now();  // 2026/02/14
        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09

        List<String> messageIds = fetchMessageIds(service, startDate, endDate);
        int batchSize = 10;
        List<EmailApplicationClassification> allClassifications = new ArrayList<>();
        for (int i = 0; i < messageIds.size(); i += batchSize) {
            List<String> batchIds = messageIds.subList(i, Math.min(i + batchSize, messageIds.size()));
            List<EmailDTO> emails = fetchEmails(service, batchIds);
            List<EmailDTO> jobApplications = filterJobApplications(emails);
            // Batch DB write
            emailService.saveAllEmails(jobApplications);
            List<EmailApplicationClassification> classifications =
                classifyApplications(jobApplications);
            jobService.saveAllApplicationEvents(classifications);
            allClassifications.addAll(classifications);

            log.info("Processed batch {} - {} ({} emails, {} job apps)",
                i, i + batchSize,
                emails.size(),
                jobApplications.size());
        }
        return allClassifications;
    }

    private List<EmailApplicationClassification> classifyApplications(List<EmailDTO> jobApplications) {
        List<String> responses = jobApplications.stream()
            .map(email -> {
                try {
                    return langChainService.processEmailApplication(email);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while processing email", e);
                }
            })
            .toList();

        return parseClassificationResponse(responses);

    }


    private List<EmailDTO> filterJobApplications(List<EmailDTO> emails) throws JsonProcessingException, InterruptedException {
        // Classify email using LLM
        List<EmailApplicationResponse> jobResponses = langChainService.filterJobApplicationSubjects(emails);

        Set<String> jobEmailIds = jobResponses.stream()
            .filter(EmailApplicationResponse::getIsJobApplication)
            .map(EmailApplicationResponse::getEmailId)
            .collect(Collectors.toSet());

        // Filter original results to ONLY job applications
        return emails.stream()
            .filter(email -> jobEmailIds.contains(email.getEmailId()))
            .toList();
    }

    private List<EmailDTO> fetchEmails(Gmail service, List<String> messageIds) throws IOException {
        List<EmailDTO> results = new ArrayList<>();
        for (String msgId : messageIds) {
            var message = service.users().messages().get("me", msgId).setFormat("full").execute();

            String subject = emailParsingService.extractHeader(message, "Subject");
            String from = emailParsingService.extractHeader(message, "from");
            String body = emailParsingService.extractPlainText(message.getPayload());
            results.add(EmailDTO.builder().emailId(msgId).from(from).subject(subject).message(body).build());
        }
        return results;
    }

    private List<String> fetchMessageIds(Gmail service, LocalDate startDate, LocalDate endDate) throws IOException {
        return getMessageIdsDateRange(service, USER, startDate.format(DateTimeFormatter.ofPattern(
            "yyyy/MM/dd")), endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), 50);
    }


    public List<EmailApplicationClassification> parseClassificationResponse(
            List<String> jsonResponses) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return jsonResponses.stream()
                .map(json -> {
                    try {
                        // Clean common LLM formatting
                        String cleanJson = cleanJsonResponse(json);
                        JsonNode node = mapper.readTree(cleanJson);

                        // Handle both single object and array formats
                        if (node.isArray() && node.size() > 0) {
                            cleanJson = mapper.writeValueAsString(node.get(0));
                        }

                        return mapper.readValue(cleanJson, EmailApplicationClassification.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse JSON: {} - {}", json, e.getMessage());
                        return EmailApplicationClassification.builder()
                                .emailId(extractEmailIdFromJson(json))  // Fallback
                                .isJobApplication(false)
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    private String extractEmailIdFromJson(String json) {
        try {
            JsonNode node = new ObjectMapper().readTree(json);
            return node.at("/emailId").asText(node.at("/email_id").asText("unknown"));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";

        // Remove common LLM markdown wrappers
        return response.replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("\\s*```\\s*$", "")
                .trim();
    }


    private List<String> getMessageIdsDateRange(Gmail service, String user, String afterDate, String beforeDate,
                                                long maxResultsPerPage) throws IOException {

        List<String> allIds = new ArrayList<>();
        String nextPageToken = null;

        // ⭐ Docs format: "in:inbox after:YYYY/MM/DD before:YYYY/MM/DD"
        String query = String.format("in:inbox after:%s before:%s", afterDate, beforeDate);

        do {
            ListMessagesResponse response =
                    service.users().messages().list(user).setQ(query) // Past 5 days exactly
                            .setMaxResults(maxResultsPerPage)     // Page size: 50
                            .setPageToken(nextPageToken)          // Pagination
                            .execute();

            if (response.getMessages() != null) {
                response.getMessages().forEach(msg -> allIds.add(msg.getId()));
            }
            nextPageToken = response.getNextPageToken();

            log.info("total ids collected {} ", allIds.size());
        } while (nextPageToken != null && allIds.size() < 500);

        return allIds;
    }

    public List<String> getLabelEmails() throws Exception {
        ListMessagesResponse response = gmailService().users().messages().list(USER).execute();
        return response.getMessages().stream().map(Message::getId).toList();

    }

    private Gmail gmailService() throws Exception {
        return gmailServiceFactory.getService();
    }

    @Retry(name = "gmailApi", fallbackMethod = "getFullMessageFallback")
    @CircuitBreaker(name = "classifyEmail")
    public String getFullMessage(String messageId) throws Exception {
        var message = gmailService().users().messages().get(USER, messageId).setFormat("full").execute();
        return emailParsingService.extractPlainText(message.getPayload());
    }

    // Optional fallback
    public String getFullMessageFallback(String messageId, Throwable t) {
        log.warn("Failed to fetch Gmail message {} after retries", messageId, t);
        return "";
    }

}