package com.dino.inbox_track.service;

import com.dino.inbox_track.db.Email;
import com.dino.inbox_track.db.EmailRepository;
import com.dino.inbox_track.dto.EmailDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class EmailService {

    private final EmailRepository emailRepository;

    public EmailService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

  public List<EmailDTO> getEmails() {
    return emailRepository.findAll().stream().map(EmailDTO::toEmailDTO).toList();
  }
    public void saveEmail(EmailDTO dto) {
        Email email = Email.builder()
                .messageId(dto.getEmailId())
                .subject(dto.getSubject())
            .date(dto.getDate().toInstant())
                .from(dto.getFrom())
                .body(dto.getMessage().substring(0, 200))
                .build();
        emailRepository.save(email);
    }

    @Transactional
    public List<Email> saveAllEmails(List<EmailDTO> jobApplications) {
    List<Email> emails = new ArrayList<>();
    for (EmailDTO dto : jobApplications) {
      emails.add(
          Email.builder()
              .messageId(dto.getEmailId())
              .subject(dto.getSubject())
              .date(dto.getDate().toInstant())
              .from(dto.getFrom())
              .body(dto.getMessage().substring(0, 200))
              .build());
    }
      return emailRepository.saveAll(emails);
  }
}
