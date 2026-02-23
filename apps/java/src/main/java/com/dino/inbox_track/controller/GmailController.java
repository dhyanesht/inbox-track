package com.dino.inbox_track.controller;

import com.dino.inbox_track.service.GmailService;
import com.dino.inbox_track.service.OllamaService;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
@Slf4j
public class GmailController {


    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final OllamaService ollamaService;
    private final GmailService gmailService;

    public GmailController(OllamaService ollamaService, GmailService gmailService) {
        this.ollamaService = ollamaService;
        this.gmailService = gmailService;
    }


    @GetMapping("/")
    public String test() {
        return "Gmail API ready!";
    }

    @GetMapping("/labels")
    public List<String> getLabels() throws Exception {
        return gmailService.getLabelNames();
    }

    @GetMapping("/recent")
    public List<Map<String, String>> getRecentEmails(@RequestParam(defaultValue = "5") int daysBack) throws Exception {
        return gmailService.getRecentEmails(daysBack);
    }


    @GetMapping("/emails")
    public List<String> getLabelEmails() throws Exception {
        return gmailService.getLabelEmails();
    }

//    @GetMapping("/messages")
//    public List<String> listMessages() throws IOException, GeneralSecurityException {
//        Gmail service = getGmailService();
//        String user = "me";
//        com.google.api.services.gmail.model.ListMessagesResponse response = service.users().messages().list(user)
//        .setMaxResults(10L).execute();
//        return response.getMessages().stream().map(msg -> msg.getId()).toList();
//    }


    // 2. Get decoded message body (plain text)

//    @GetMapping("/messages/{messageId}")
//    public com.google.api.services.gmail.model.Message getMessage(@PathVariable String messageId) throws
//    IOException, GeneralSecurityException {
//        Gmail service = getGmailService();
//        String user = "me";
//        return service.users().messages().get(user, messageId).setFormat("metadata").execute();
//    }
    // 3. Helper method to decode base64url content

    @GetMapping("/messages/{messageId}/full")
    public String getFullMessage(@PathVariable String messageId) throws Exception {
        return gmailService.getFullMessage(messageId);
    }

//    @GetMapping("/messages/{messageId}/body")
//    public String getMessageBody(@PathVariable String messageId) throws IOException, GeneralSecurityException {
//        Gmail service = getGmailService();
//        String user = "me";
//
//        var message = service.users().messages().get(user, messageId).setFormat("full").execute();
//
//        return decodeMessagePayload(message.getPayload());
//    }


//    @GetMapping("/recent")
//    public List<Map<String, Object>> getRecentEmails(@RequestParam(defaultValue = "5") int daysBack) throws
//    Exception {
//
//        Gmail service = getGmailService();
//        String user = "me";
//
//        // Calculate dates (past 5 days = Feb 9-14, 2026)
//        LocalDate endDate = LocalDate.now();  // 2026/02/14
//        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09
//
//        List<String> recentIds = getMessageIdsDateRange(service, user, startDate.format(DateTimeFormatter.ofPattern
//        ("yyyy/MM/dd")),
//                // "2026/02/09"
//                endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),    // "2026/02/14"
//                50);
//        List<Map<String, Object>> results = new ArrayList<>();
//        for (int i = 0; i < Math.min(15, recentIds.size()); i++) {
//            String msgId = recentIds.get(i);
//            var fullMsg = service.users().messages().get(user, msgId).setFormat("full").execute();
//
//            String subject = extractHeader(fullMsg, "Subject");
//            String sender = cleanSender(extractHeader(fullMsg, "From"));
//            String cleanText = extractPlainText(fullMsg.getPayload());
//
//            Email email = Email.builder().subject(subject).body(cleanText).messageId(msgId).build();
//
//            String label = ollamaService.classifyEmail(cleanText, subject);
//
//            results.add(Map.of("id", msgId, "subject", subject, "sender", sender, "body", cleanText, "aiLabel",
//            label));
//        }
//
//        return results;
//    }

//    @GetMapping("/recent2")
//    public List<Map<String, String>> getRecentEmails2(@RequestParam(defaultValue = "5") int daysBack) throws
//    Exception {
//
//        Gmail service = getGmailService();
//        String user = "me";
//
//        // Calculate dates (past 5 days = Feb 9-14, 2026)
//        LocalDate endDate = LocalDate.now();  // 2026/02/14
//        LocalDate startDate = endDate.minusDays(daysBack);  // 2026/02/09
//
//        List<String> recentIds = getMessageIdsDateRange(service, user, startDate.format(DateTimeFormatter.ofPattern
//        ("yyyy/MM/dd")),
//                // "2026/02/09"
//                endDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),    // "2026/02/14"
//                50);
//        log.info("Recent ids {}", recentIds.size());
//        List<Map<String, String>> results = new ArrayList<>();
//        List<Email> emailList = new ArrayList<>();
//        recentIds = recentIds.subList(0, 5);
//        for (int i = 0; i < recentIds.size(); i++) {
//            String msgId = recentIds.get(i);
//            Message fullMsg;
//            try {
//                fullMsg = service.users().messages().get(user, msgId).setFormat("full").execute();
//            } catch (NullPointerException exception) {
//                System.out.println("msgId = " + msgId);
//                System.out.println("exception = " + exception);
//                throw exception;
//            }
//
//            String subject = extractHeader(fullMsg, "Subject");
//            String sender = cleanSender(extractHeader(fullMsg, "From"));
//            String cleanText = extractPlainText(fullMsg.getPayload());
//            System.out.println("subject = " + subject);
//            emailList.add(Email.builder().subject(subject).body(cleanText).messageId(msgId).build());
//
////            String label = ollamaService.classifyEmail(cleanText, subject);
//
////            results.add(Map.of(
////                    "id", msgId, "subject", subject, "sender", sender,
////                    "body", cleanText, "aiLabel", label
////            ));
//
//        }
//        Map<String, String> emailSubjects = emailList.stream().collect(Collectors.toMap(Email::getMessageId,
//        Email::getSubject));
//
//        List<Map<String, String>> emailsToClassify = ollamaService.filterJobApplicationSubjects(emailSubjects);
//
//        results = emailsToClassify;
//
//        return results;
//    }


//    @GetMapping("/top5")
//    public List<Map<String, Object>> getTop5Emails() throws IOException, GeneralSecurityException {
//        Gmail service = getGmailService();
//        String user = "me";
//
//        // Get top 5 message IDs from inbox
//        var msgList = service.users().messages().list(user).setMaxResults(15L).setLabelIds(Collections
//        .singletonList("INBOX")).execute();
//
//        List<Map<String, Object>> results = new ArrayList<>();
//
//        for (var msg : msgList.getMessages()) {
//            try {
//                var fullMsg = service.users().messages().get(user, msg.getId()).setFormat("full").execute();
//
//                String subject = extractHeader(fullMsg, "Subject");
//                String sender = extractHeader(fullMsg, "From");
////                String body = cleanBody(decodeMessagePayload(fullMsg.getPayload()));
//                String cleanText = extractPlainText(fullMsg.getPayload());
//
//                String classificaiton = ollamaService.classifyEmail2(cleanText, subject);
//                results.add(Map.of(

    /// /                        "id", msg.getId(),
//                        "subject", subject, "sender", cleanSender(sender), "bodyPreview", cleanText, "date",
//                        extractHeader(fullMsg, "Date"), "classificaiton", classificaiton));
//            } catch (Exception e) {
//                results.add(Map.of("id", msg.getId(), "error", e.getMessage()));
//            }
//        }
//
//        return results;
//    }


    // Helper methods
    private String extractHeader(com.google.api.services.gmail.model.Message message, String headerName) {
        return message.getPayload().getHeaders().stream().filter(h -> headerName.equals(h.getName())).map(com.google.api.services.gmail.model.MessagePartHeader::getValue).findFirst().orElse("N/A");
    }

    private String cleanSender(String sender) {
        return sender.replaceAll("=?utf-8\\?B\\?[^?]+\\?=", "").replaceAll("=?UTF-8\\?B\\?[^?]+\\?=", "").replaceAll(
                "<[^>]+>", "").trim();
    }

    private String cleanBody(String rawBody) {
        return rawBody.replaceAll("\\\\r\\\\n|\\\\n|\\\\r|\\r\\n|\\n|\\r", " ").replaceAll("\\s+", " ").trim();
    }

//    private String extractPlainText(com.google.api.services.gmail.model.MessagePart payload) {
//        StringBuilder text = new StringBuilder();
//        extractTextFromPart(payload, text);
//
//        String result = text.toString().replaceAll("<[^>]+>", "")  // Strip HTML
//                .replaceAll("\\s+", " ")     // Normalize space
//                .replaceAll("(http[^\\s]+)", "")  // Remove URLs
//                .trim();
//
//        return result.length() > 0 ? result : "No readable text";
//    }

//    private void extractTextFromPart(com.google.api.services.gmail.model.MessagePart part, StringBuilder text) {
//        if ("text/plain".equals(part.getMimeType()) && part.getBody().getData() != null) {
//            text.append(decodeBase64Url(part.getBody().getData()));
//            return;
//        }
//
//        if (part.getParts() != null) {
//            for (var child : part.getParts()) {
//                extractTextFromPart(child, text);
//            }
//        }
//    }


}
