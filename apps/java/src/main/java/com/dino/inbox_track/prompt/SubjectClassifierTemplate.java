package com.dino.inbox_track.prompt;

import com.dino.inbox_track.dto.EmailApplication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import lombok.Getter;

import java.util.List;

public class SubjectClassifierTemplate {


    public static class EmailSubjectData {
        public String emailId;
        public String subject;

        public EmailSubjectData(EmailApplication email) {
            this.emailId = email.getEmailId();
            this.subject = email.getSubject();
        }
    }


    @StructuredPrompt({"""
            You are an email subject line classifier that detects only job application or job opportunity emails.
            
            Your task:
            - Input: A list of email subjects.
            - Output: ONLY those subjects clearly about:
              - Someone applying for a job, position, or interview, OR
              - A recruiter/company contacting someone about a job, interview, or hiring process.
            
            For each input subject, output a JSON object with:
            - "emailId": the original id exactly as given.
            - "subject": the original subject exactly as given.
            - "isJobApplication": a boolean indicating:
              - true if clearly job-related as described above.
              - false otherwise.
            
            Classification Rules:
            - Mark as true if the subject includes: apply, application, interview, candidate, resume, CV, position, role, job, opportunity, recruiter, or a specific job title.
            - Mark as false if it's a newsletter, promotion, invoice, product update, receipt, generic career advice, or ambiguous content not clearly about a job or hiring.
            - Classification should be case-insensitive.
            
            Ambiguity Rule:
            - If it's unclear or not explicitly job-related, set "isJobApplication": false.
            
            Output Format:
            Return ONLY a valid JSON array. No explanations, no extra text.
            
            Example:
            Input:
            [
              {"emailId": "1", "subject": "Interview invitation for Backend Engineer"},
              {"emailId": "2", "subject": "Your monthly newsletter"}
            ]
            
            Output:
            [
              {"emailId": "1", "subject": "Interview invitation for Backend Engineer", "isJobApplication": true},
              {"emailId": "2", "subject": "Your monthly newsletter", "isJobApplication": false}
            ]
            
            Now classify the following subjects:
            
            {{emailSubjectsJson}}
            
            Return ONLY a valid JSON array of results.
            """})
    public static class SubjectClassifierPrompt {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final String emailSubjectsJson;

        public SubjectClassifierPrompt(List<EmailApplication> emailSubjects) {
            List<EmailSubjectData> subjectData = emailSubjects.stream()
                    .map(EmailSubjectData::new)
                    .toList();
            this.emailSubjectsJson = toJson(subjectData);
            System.out.println(emailSubjectsJson);
        }

        private static String toJson(Object value) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize emailSubjects", e);
            }
        }

        public String getEmailSubjectsJson() {
            return emailSubjectsJson;
        }

    }


}
