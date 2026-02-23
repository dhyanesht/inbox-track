package com.dino.inbox_track.service;

import com.dino.inbox_track.client.GmailClientFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private static final String USER = "me";
    private final GmailClientFactory gmailClientFactory;
    private final EmailParsingService emailParsingService;
    private final OllamaService ollamaService;

    public List<String> getLabelNames() throws Exception {
        var response = gmailService().users().labels().list("me").execute();
        return response.getLabels().stream().map(Label::getName).toList();

    }

    public List<Map<String, String>> getRecentEmails(int daysBack) throws Exception {

        var service = gmailService();

        LocalDate endDate = LocalDate.now();  // 2026/02/14
        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09

        List<String> messageIds = getMessageIdsDateRange(service, USER, startDate.format(DateTimeFormatter.ofPattern(
                "yyyy/MM/dd")), endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), 50);

        List<Map<String, String>> results = new ArrayList<>();
//        messageIds = messageIds.subList(0, 2);
        for (String msgId : messageIds) {
            var message = service.users().messages().get("me", msgId).setFormat("full").execute();

            String subject = emailParsingService.extractHeader(message, "Subject");
            String body = emailParsingService.extractPlainText(message.getPayload());
            results.add(Map.of("id", msgId, "subject", subject, "body", body));
        }
        // save email details to database

        // classify email using llm
//        Map<String, String> subjects = results.stream()
//                .flatMap(stringStringMap -> stringStringMap.entrySet().stream())
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (v1, v2) -> v2
//                ));
        Map<String, String> subjects = results.stream().collect(Collectors.toMap(
                m -> m.get("id").replace('1', 'd').replace('a', 'd'), m -> m.get("subject")));
        List<Map<String, String>> classiedSubjects = ollamaService.filterJobApplicationSubjects(subjects);

        return classiedSubjects;
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