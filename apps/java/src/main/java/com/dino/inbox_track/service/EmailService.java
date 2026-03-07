package com.dino.inbox_track.service;

import com.dino.inbox_track.db.Email;
import com.dino.inbox_track.db.EmailRepository;
import com.dino.inbox_track.dto.EmailDTO;
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

    public void saveEmail(EmailDTO dto) {
        Email email = Email.builder()
                .messageId(dto.getEmailId())
                .subject(dto.getSubject())
                .date(dto.getDate())
                .from(dto.getFrom())
                .body(dto.getMessage().substring(0, 200))
                .application(null) // Link later via business logic
                .build();
        emailRepository.save(email);
    }

}
