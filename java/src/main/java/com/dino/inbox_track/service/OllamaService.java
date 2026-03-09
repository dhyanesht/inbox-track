package com.dino.inbox_track.service;

import com.dino.inbox_track.client.GroqLangChainService;
import com.dino.inbox_track.dto.OllamaGenerateRequest;
import com.dino.inbox_track.dto.OllamaGenerateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma3:4b";
    private final RestClient ollamaRestClient;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    GroqLangChainService groqLangChainService;

    public OllamaService(RestClient ollamaRestClient) {
        this.ollamaRestClient = ollamaRestClient;
    }

    public String classifyEmailTest(String emailBody, String subject) {
        return "This is a response message";
    }

    public String classifyEmailFallback() {
        return "This is fallback";
    }

    @CircuitBreaker(name = "classifyEmail", fallbackMethod = "classifyEmailFallback")
    public String classifyEmail(String emailBody, String subject) {
        String prompt = String.format(
                """
                        Analyze this email and return ONLY a Gmail label name (like "work", "promotions", "social", "important", "spam").
                        Respond with just the label name, nothing else.
                        
                        Subject: %s
                        Body: %s
                        """, subject, emailBody
        );
        ObjectNode request = mapper.createObjectNode();
        request.put("model", MODEL);
        request.put("prompt", prompt);
        request.put("stream", false);

        ObjectNode response = ollamaRestClient.post()
                .body(request)
                .retrieve()
                .body(ObjectNode.class);
        return response.get("response").asText().trim();

    }

    public String classifyEmail2(String emailBody, String subject) throws JsonProcessingException {
        String prompt = String.format(
                """
                        role: "system",
                        content: `You extract job application information from text. Extract:
                        - company_name (required)
                        - position (required)
                        - location (optional, extract city/state/country or "Remote" or "United States (Remote)")
                        - application_date (optional, in ISO format YYYY-MM-DD, default to today if not found)
                        - email_type (optional: "thank_you", "interview", "offer", "rejection", "other" - only if this is an update email about existing application)
                            Return ONLY valid JSON with these fields. If company_name or position cannot be determined, return {"error": "insufficient_info"}.`
                        },
                        {
                        role: "user",
                        content: `Extract job application info from: %s `
                        }
                        """, emailBody
        );
        ObjectNode request = mapper.createObjectNode();
        request.put("model", MODEL);
        request.put("prompt", prompt);
        request.put("stream", false);


        ObjectNode response = ollamaRestClient.post()
                .body(request)
                .retrieve()
                .body(ObjectNode.class);
        return response.get("response").asText().trim();

    }


    public List<Map<String, String>> filterJobApplicationSubjects(Map<String, String> emailSubjects) throws JsonProcessingException {
        // emails: List of {id, subject}
        StringBuilder subjectsText = new StringBuilder();
        for (Map.Entry<String, String> entry : emailSubjects.entrySet()) {
            subjectsText.append(String.format("ID: %s | Subject: %s ;",
                    entry.getKey(), entry.getValue()));
        }

//        String prompt = """
//                You are a job application email classifier.
//
//                From a list of email subjects, identify which emails are potential job applications.
//
//                Return ONLY a valid JSON array, with no extra text, no markdown, and no code fences.
//                Example format:
//                [
//                  {"id": "123", "subject": "Junior Java Developer role"},
//                  {"id": "456", "subject": "Application for Software Engineer"}
//                ]
//
//                Subjects: %s
//                """.formatted(subjectsText.toString());

//        String prompt = """
//                You are an email subject line classifier that ONLY detects potential JOB APPLICATION or JOB OPPORTUNITY emails.
//
//                Your task:
//                - Input: A list of email subjects.
//                - Output: ONLY those subjects that are clearly about:
//                  - Someone applying for a job, a role, a position, an interview, or
//                  - A recruiter/company contacting someone about a job or interview.
//
//                STRICT rules:
//                - DO NOT include newsletters, promotions, discounts, coupons, marketing emails, product updates, policy updates, tax notices, receipts, ride offers, or generic learning/community updates.
//                - DO NOT include personal greetings or holiday messages unless they clearly mention a job, role, interview, recruitment, or hiring.
//                - If a subject is ambiguous and does NOT clearly indicate a job or job application context, EXCLUDE it.
//                - Focus on words like: "apply", "application", "candidate", "resume", "CV", "interview", "hiring", "position", "role", "job", "vacancy", "opening", "Senior Backend Java Developer", "Software Engineer", etc.
//                - Subjects about careers or job search tips WITHOUT a specific position or application should also be EXCLUDED.
//
//                Output format:
//                - Return ONLY a valid JSON array.
//                - NO explanations, NO markdown, NO code fences, NO extra keys.
//                - Each element must have:
//                  - "id": the original id exactly as given.
//                  - "subject": the original subject line exactly as given.
//
//                Example:
//                [
//                  {"id": "19c4f132e9466130", "subject": "Fwd: Senior Backend Java Developer- "},
//                ]
//
//                Now classify the following subjects and return ONLY the JSON array as specified:
//
//                %s
//                """.formatted(subjectsText.toString());

        String promptTemplate =
                """     
                        You are an email subject line classifier that ONLY detects potential JOB APPLICATION or JOB OPPORTUNITY emails.
                        
                        Your task:
                        - Input: A list of email subjects.
                        - Output: ONLY those subjects that are clearly about:
                          - Someone applying for a job, a role, a position, an interview, or
                          - A recruiter/company contacting someone about a job or interview.
                        
                        Classification requirement:
                        - For each input subject, you must output a JSON object with:
                          - "id": the original id exactly as given.
                          - "subject": the original subject line exactly as given.
                          - "IsJobApplication": a boolean:
                            - true if the subject is clearly about a job application, job opportunity, or interview.
                            - false otherwise.
                        
                        STRICT rules for setting IsJobApplication:
                        - Set "IsJobApplication": true ONLY if the subject line clearly indicates:
                          - A person applying for a job, role, position, or interview.
                          - A recruiter, hiring manager, or company contacting someone about a job, role, position, interview, or hiring process.
                        
                        Positive signals (usually IsJobApplication = true):
                        - Subjects that clearly reference:
                          - "apply", "application", "applied", "applicant"
                          - "candidate", "shortlisted", "selected" (in a hiring context)
                          - "resume", "CV", "curriculum vitae", "portfolio" (when tied to a role or position)
                          - "interview", "screening", "call with recruiter", "hiring manager"
                          - "job", "position", "role", "vacancy", "opening", "opportunity" (when clearly about employment)
                          - Specific job titles such as "Senior Backend Java Developer", "Software Engineer", "Data Scientist", etc.
                        - Subjects that clearly indicate scheduling or status of an interview or application:
                          - "Interview invitation for...", "Interview confirmation", "Your application for...", "Regarding your application to..."
                          - "We are interested in your profile for...", "Job opportunity at...", "New role that matches your profile"
                        
                        STRICT EXCLUSION rules (IsJobApplication = false):
                        - DO NOT mark as job-related (IsJobApplication must be false) for:
                          - Newsletters, promotions, discounts, coupons, or marketing emails.
                          - Product updates, feature announcements, release notes.
                          - Policy updates, terms of service updates, privacy updates.
                          - Tax notices, receipts, invoices, payment confirmations, subscription renewals.
                          - Ride offers, delivery updates, reservation confirmations, order tracking.
                          - Generic learning, community, or career tips without a specific job or application (e.g., "Improve your resume", "Interview tips", "Grow your career").
                          - Generic platform emails like "New jobs you may like", "Recommended roles for you" when they are clearly bulk recommendations and not a specific application or recruiter outreach.
                        
                        Personal/holiday messages:
                        - DO NOT mark as job-related unless they explicitly mention:
                          - A job, role, position, application, interview, recruitment, or hiring.
                        - Examples that should be false:
                          - "Happy New Year!", "Season's Greetings", "Hello from our team" (with no hiring context).
                        
                        Ambiguity rule:
                        - If the subject is ambiguous and DOES NOT clearly indicate a job or job application context, set:
                          - "IsJobApplication": false.
                        - When in doubt, prefer false.
                        
                        Careers content rule:
                        - Subjects about general career advice, job search tips, or learning (without a specific role/application) MUST be:
                          - "IsJobApplication": false.
                        - Examples:
                          - "Tips to get your dream job", "How to ace your interview", "Improve your CV in 5 steps" → false.
                        
                        Output format:
                        - Return ONLY a valid JSON array.
                        - NO explanations, NO markdown, NO code fences, NO extra keys.
                        - Each element must be an object of the form:
                          {
                            "id": "<original id>",
                            "subject": "<original subject line>",
                            "IsJobApplication": <true or false>
                          }
                        
                        Example behavior:
                        - Input subjects:
                          - id: "19c4f132e9466130", subject: "Fwd: Senior Backend Java Developer- "
                          - id: "a1", subject: "Your monthly newsletter from XYZ"
                        - Output:
                        [
                          {"id": "19c4f132e9466130", "subject": "Fwd: Senior Backend Java Developer- ", "IsJobApplication": true},
                          {"id": "a1", "subject": "Your monthly newsletter from XYZ", "IsJobApplication": false}
                        ]
                        
                        Now classify the following subjects and return ONLY the JSON array as specified:
                        
                        %s
                        """.stripIndent();
        ArrayNode subjectsArray = mapper.createArrayNode();
        for (Map.Entry<String, String> emailSubjectEntry : emailSubjects.entrySet()) {
            ObjectNode subjectNode = mapper.createObjectNode();
            subjectNode.put("id", emailSubjectEntry.getKey());
            subjectNode.put("subject", emailSubjectEntry.getValue());
            subjectsArray.add(subjectNode);
        }
        String prompt = promptTemplate.formatted(mapper.writeValueAsString(subjectsArray));
        System.out.println("prompt = " + prompt);
//        String response = generate(MODEL, prompt);
        String response = groqLangChainService.classifyEmail(prompt);
        // Parse JSON array of {id, subject}
        String jsonArrayString = response.trim();
        String raw = jsonArrayString.trim();

        // Remove markdown code fences if present
        if (raw.startsWith("```")) {
            // remove first line (``` or ```json)
            int firstNewline = raw.indexOf('\n');
            if (firstNewline != -1) {
                raw = raw.substring(firstNewline + 1);
            }
            // remove trailing ```
            int lastFence = raw.lastIndexOf("```");
            if (lastFence != -1) {
                raw = raw.substring(0, lastFence);
            }
            raw = raw.trim();
        }

//        System.out.println("jsonArrayString = " + jsonArrayString);
        System.out.println("jsonArrayString = " + raw);
        return mapper.readValue(raw, new TypeReference<List<Map<String, String>>>() {
        });
    }

    public String generate(String model, String prompt) {
        OllamaGenerateRequest request = new OllamaGenerateRequest(
                model,
                prompt,
                false   // stream=false -> single JSON response
        );

        OllamaGenerateResponse resp = ollamaRestClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaGenerateResponse.class);

        return resp != null ? resp.response() : null;
    }


}
