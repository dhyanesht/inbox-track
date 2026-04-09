package com.dino.inbox_track.service;

import com.dino.inbox_track.client.GmailServiceFactory;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.exception.EmailFetchException;
import com.dino.inbox_track.prompt.LLMMessageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
public class GmailService implements EmailFetcher {

    private static final String USER = "me";
    private final GmailServiceFactory gmailServiceFactory;
    private final Gmail gmail;
    private final EmailParsingService emailParsingService;
    private final EmailService emailService;
    private final JobService jobService;
    private final OllamaService ollamaService;
    private final LLMMessageParser llmMessageParser;
    private final LangChainService langChainService;
    private final ObjectMapper mapper;
    private final CheckpointService checkpointService;


    public List<String> getLabelNames() {
        ListLabelsResponse response = null;
        try {
            response = gmail.users().labels().list("me").execute();
        } catch (IOException e) {
            throw new EmailFetchException("Failed to fetch Label names from provider", e);
        }
        return response.getLabels().stream().map(Label::getName).toList();

    }

    public List<EmailApplicationClassification> getRecentEmails(int daysBack) throws Exception {


        LocalDate endDate = LocalDate.now();  // 2026/02/14
        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09
        List<String> messageIds = fetchMessageIds(startDate, endDate);
        // Use checkpoint service to filter already processed emails
        Set<String> processedEmails = checkpointService.getProcessedEmailIds();
        messageIds = messageIds.stream().filter(id -> !processedEmails.contains(id)).toList();
        log.info("Filtered {} using checkpoint service", messageIds.size());

        int batchSize = 10;
        List<EmailApplicationClassification> allClassifications = new ArrayList<>();
        for (int i = 0; i < messageIds.size(); i += batchSize) {
            List<String> batchIds = messageIds.subList(i, Math.min(i + batchSize, messageIds.size()));
            List<EmailDTO> emails = fetchEmails(batchIds);
            List<EmailDTO> jobApplications = filterJobApplications(emails);
            // Batch DB write
            emailService.saveAllEmails(jobApplications);
            List<EmailApplicationClassification> classifications = classifyApplications(jobApplications);
            log.info(classifications.toString());
            jobService.saveAllApplicationEvents(classifications);
            allClassifications.addAll(classifications);

            log.info("Processed {} / {} (Current Batch: {} emails, {} job apps)",
                i + batchSize, messageIds.size(),
                emails.size(),
                jobApplications.size());
        }
        return allClassifications;
    }

    private List<EmailApplicationClassification> classifyApplications(List<EmailDTO> jobApplications) {
        List<String> responses = jobApplications.stream()
            .map(langChainService::processEmailApplication)
            .toList();

        return llmMessageParser.parseClassificationResponse(responses);

    }

    public List<EmailDTO> filterJobApplications(List<EmailDTO> emails) {
        List<EmailApplicationResponse> jobResponses;
        try {
            jobResponses = langChainService.filterJobApplicationSubjects(emails);
        } catch (JsonProcessingException e) {
            throw new EmailFetchException("Failed to parse AI response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailFetchException("AI processing was interrupted", e);
        }

        Set<String> jobEmailIds = jobResponses.stream()
            .filter(resp -> Boolean.TRUE.equals(resp.getIsJobApplication()))
            .map(EmailApplicationResponse::getEmailId)
            .collect(Collectors.toSet());

        // Filter original results to ONLY job applications
        return emails.stream()
            .filter(email -> jobEmailIds.contains(email.getEmailId()))
            .toList();
    }

    @Override
    public List<String> fetchMessageIds(int daysBack) {
        LocalDate endDate = LocalDate.now();       // 2026/02/14
        LocalDate startDate = endDate.minusDays(daysBack); // 2026/02/09
        try {
            return fetchMessageIds(startDate, endDate);
        } catch (IOException e) {
            throw new EmailFetchException("Failed to fetch message IDs from provider", e);
        }
    }

    @Override
    public List<EmailDTO> fetchEmails(List<String> messageIds) {
        List<EmailDTO> results = new ArrayList<>();
        for (String msgId : messageIds) {
            Message message = null;
            try {
                message = gmail.users().messages().get(USER, msgId).setFormat("full").execute();
            } catch (IOException e) {
                throw new EmailFetchException("Failed to fetch message IDs from provider", e);
            }
            ZonedDateTime dateTime = Instant.ofEpochMilli(message.getInternalDate()).atZone(ZoneId.of("UTC"));
            String from = emailParsingService.extractHeader(message, "From");
            String subject = emailParsingService.extractHeader(message, "Subject");
            String body = emailParsingService.extractPlainText(message.getPayload());
            results.add(EmailDTO.builder().emailId(msgId).date(dateTime).from(from).subject(subject).message(body).build());
        }
        return results;
    }

    public List<String> fetchMessageIds(LocalDate startDate, LocalDate endDate) throws IOException {
        return getMessageIdsDateRange(gmail, startDate.format(DateTimeFormatter.ofPattern(
            "yyyy/MM/dd")), endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), 50);
    }


    private List<String> getMessageIdsDateRange(Gmail service, String afterDate, String beforeDate,
                                                long maxResultsPerPage) throws IOException {

        List<String> allIds = new ArrayList<>();
        String nextPageToken = null;

        // Docs format: "in:inbox after:YYYY/MM/DD before:YYYY/MM/DD"
        String query = String.format("in:inbox after:%s before:%s", afterDate, beforeDate);

        do {
            ListMessagesResponse response = service.users().messages().list(GmailService.USER).setQ(query) // Past 5 days exactly
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

    public List<String> getLabelEmails() throws IOException {
        ListMessagesResponse response = gmail.users().messages().list(USER).execute();
        return response.getMessages().stream().map(Message::getId).toList();

    }


    //    @Retry(name = "gmailApi", fallbackMethod = "getFullMessageFallback")
//    @CircuitBreaker(name = "classifyEmail")
    public String getFullMessage(String messageId) throws IOException {
        var message = gmail.users().messages().get(USER, messageId).setFormat("full").execute();
        return emailParsingService.extractPlainText(message.getPayload());
    }

    // Optional fallback
    public String getFullMessageFallback(String messageId, Throwable t) {
        log.warn("Failed to fetch Gmail message {} after retries", messageId, t);
        return "";
    }

}