package com.dino.inbox_track.controller;

import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.service.GmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gmail")
@Slf4j
public class GmailController {


    private final GmailService gmailService;

    public GmailController(GmailService gmailService) {
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
    public List<EmailApplicationClassification> getRecentEmails(@RequestParam(defaultValue = "5") int daysBack) throws Exception {
        return gmailService.getRecentEmails(daysBack);
    }


    @GetMapping("/emails")
    public List<String> getLabelEmails() throws Exception {
        return gmailService.getLabelEmails();
    }

    @GetMapping("/messages/{messageId}/full")
    public String getFullMessage(@PathVariable String messageId) throws Exception {
        return gmailService.getFullMessage(messageId);
    }



}
