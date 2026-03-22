package com.dino.inbox_track.prompt;

import com.dino.inbox_track.dto.EmailDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.input.structured.StructuredPrompt;

public class MessageClassifierTemplate {

    @StructuredPrompt({"""
            You are an email classifier that extracts job application details for a job tracker.
            
            Input: A single email object with:
              "emailId", "from", "to", "subject", "message".
            
            Output: A single JSON object that:
              - Includes all original fields: "emailId", "from", "to", "subject".
              - Add:
                "IsJobApplication": true if clearly about a job application or opportunity, false otherwise.
              - If IsJobApplication is true, also add:
                "Company"
                "Country"
                "PositionTitle"
                "PositionLocation"
                "Salary"
                "ApplicationStage"            // e.g., "application submitted", "interview", "offer"
                "JobId"                       // any reference ID mentioned
                "ApplicationStatus"           // e.g., "submitted", "review", "rejected", "offer"
                "NextStep"                    // e.g., "awaiting feedback", "interview scheduled"
                "ApplicationDate"             // when the application was sent, if clear
                "Deadline"                    // closing or response deadline, if mentioned
                "RemotePolicy"                // e.g., "Remote", "On-site", "Hybrid"
                "EmploymentType"              // e.g., "full-time", "contract", "internship"
                "ExperienceLevel"             // e.g., "Junior", "Mid-level", "Senior"
                "HiringManager"               // name of the person, if mentioned
              If any field is not present, omit it or set it to null.
            
            Classification Rules:
            - Mark true if content mentions: apply, application, interview, candidate, resume, CV, position, role, job, opportunity, recruiter, or a specific job title (case‑insensitive).
            - Mark false if it's a newsletter, promotion, invoice, product update, receipt, generic career advice, or ambiguous content.
            
            Output Format:
            Return ONLY a valid JSON object. No explanations, no extra text.
            
            Example:
            Input:
            {
              "emailId": "1",
              "from": "hiring@acme.com",
              "to": "you@example.com",
              "subject": "Interview invitation for Backend Engineer",
              "message": "Hi, you've been invited to interview for Backend Engineer at Acme Corp in New York. Deadline: 2026-03-10."
            }
            
            Output:
            {
              "emailId": "1",
              "from": "hiring@acme.com",
              "to": "you@example.com",
              "subject": "Interview invitation for Backend Engineer",
              "isJobApplication": true,
              "company": "Acme Corp",
              "country": "USA",
              "positionTitle": "Backend Engineer",
              "positionLocation": "New York",
              "applicationStage": "interview invitation",
              "applicationStatus": "review",
              "nextStep": "awaiting feedback",
              "deadline": "2026-03-10",
              "remotePolicy": "Hybrid",
              "employmentType": "full-time",
              "experienceLevel": "Mid-level",
              "hiringManager": "Alex Smith"
            }
            
            Now classify and extract from this email:
            
            {{emailJson}}
            
            Return ONLY a valid JSON object.
            
            """})
    public static class MessageClassifierPrompt {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final String emailJson;

        public MessageClassifierPrompt(EmailDTO emailSubjects) {
            this.emailJson = toJson(emailSubjects);
        }

        private static String toJson(Object value) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize emailJson", e);
            }
        }

    }


}
