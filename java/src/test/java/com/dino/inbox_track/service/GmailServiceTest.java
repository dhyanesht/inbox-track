package com.dino.inbox_track.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dino.inbox_track.dto.EmailApplicationResponse;
import com.dino.inbox_track.dto.EmailDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GmailServiceTest {

  @Mock
  private LangChainService langChainService;

  @InjectMocks
  private GmailService gmailService; // the class containing filterJobApplications

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }


  @Test
  void testFilterJobApplications() throws JsonProcessingException, InterruptedException {
    // Arrange: create test emails
    EmailDTO email1 = EmailDTO.builder().emailId("1").subject("Job at Google").build();
    EmailDTO email2 = EmailDTO.builder().emailId("2").subject("Meeting notes").build();
    EmailDTO email3 = EmailDTO.builder().emailId("3").subject("Job at Amazon").build();

    List<EmailDTO> emails = Arrays.asList(email1, email2, email3);

    // Arrange: mock LLM response
    EmailApplicationResponse resp1 = new EmailApplicationResponse("1", "Job at Google", true);
    EmailApplicationResponse resp2 = new EmailApplicationResponse("2", "Meeting notes", false);
    EmailApplicationResponse resp3 = new EmailApplicationResponse("3", "Job at Amazon", true);

    List<EmailApplicationResponse> jobResponses = Arrays.asList(resp1, resp2, resp3);

    when(langChainService.filterJobApplicationSubjects(emails)).thenReturn(jobResponses);

    // Act
    List<EmailDTO> result = gmailService.filterJobApplications(emails);

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.contains(email1));
    assertTrue(result.contains(email3));
    assertFalse(result.contains(email2));

    // Verify the LLM service was called once
    verify(langChainService, times(1)).filterJobApplicationSubjects(emails);
  }

  @Test
  void getLabelNames() {
  }

  @Test
  void getRecentEmails() {
  }

  @Test
  void parseClassificationResponse() {
  }

  @Test
  void getLabelEmails() {
  }

  @Test
  void getFullMessage() {
  }

  @Test
  void getFullMessageFallback() {
  }
}