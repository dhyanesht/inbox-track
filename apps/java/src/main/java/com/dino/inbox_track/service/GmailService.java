package com.dino.inbox_track.service;

import com.dino.inbox_track.client.GmailClientFactory;
import com.dino.inbox_track.dto.EmailApplication;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private static final String USER = "me";
    private final GmailClientFactory gmailClientFactory;
    private final EmailParsingService emailParsingService;
    private final OllamaService ollamaService;
    @Autowired
    private final LangChainService langChainService;

    public List<String> getLabelNames() throws Exception {
        var response = gmailService().users().labels().list("me").execute();
        return response.getLabels().stream().map(Label::getName).toList();

    }

    public List<EmailApplicationClassification> getRecentEmails(int daysBack) throws Exception {

        var service = gmailService();

        LocalDate endDate = LocalDate.now();  // 2026/02/14
        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09

        List<String> messageIds = getMessageIdsDateRange(service, USER, startDate.format(DateTimeFormatter.ofPattern(
                "yyyy/MM/dd")), endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), 50);

        List<EmailApplication> results = new ArrayList<>();
        for (String msgId : messageIds) {
            var message = service.users().messages().get("me", msgId).setFormat("full").execute();

            String subject = emailParsingService.extractHeader(message, "Subject");
            String body = emailParsingService.extractPlainText(message.getPayload());
            results.add(EmailApplication.builder().emailId(msgId).subject(subject).message(body).build());
        }
        // save email details to database
        // Classify email using LLM

        List<EmailApplicationResponse> jobResponses = langChainService.filterJobApplicationSubjects(results);
        jobResponses = jobResponses.stream().filter(EmailApplicationResponse::getIsJobApplication).toList();

        // Create Map for lookup (only job applications)
        Map<String, EmailApplicationResponse> jobResponseByEmailId = jobResponses.stream()
                .collect(Collectors.toMap(
                        EmailApplicationResponse::getEmailId,
                        r -> r,
                        (a, b) -> b
                ));

        // Filter original results to ONLY job applications
        List<EmailApplication> jobApplications = results.stream()
                .filter(email -> jobResponseByEmailId.containsKey(email.getEmailId()))  // Only job apps
                .map(email -> {
                    EmailApplicationResponse resp = jobResponseByEmailId.get(email.getEmailId());
                    return EmailApplication.builder()
                            .emailId(email.getEmailId())
                            .subject(email.getSubject())
                            .message(email.getMessage())
                            .build();
                })
                .toList();


        // Then call your function on each item
        List<String> llmMessageClassificationResponse = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        jobApplications.forEach(emailApp -> {
            try {
                int current = counter.incrementAndGet();
                log.info("Processing {}/{}: {}",
                        current, jobApplications.size(), emailApp.getSubject());

                var response = langChainService.processEmailApplication(emailApp);
                llmMessageClassificationResponse.add(response);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        List<EmailApplicationClassification> classificationList =
                parseClassificationResponse(llmMessageClassificationResponse);

        return classificationList;
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
        return gmailClientFactory.getService();
    }

    public String getFullMessage(String messageId) throws Exception {
        var message = gmailService().users().messages().get(USER, messageId).setFormat("full").execute();
        return emailParsingService.extractPlainText(message.getPayload());
    }

}