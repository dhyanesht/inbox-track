package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.ApplicationEventDTO;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.dto.JobApplicationDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

  private final JobService jobService;
  private final EmailService emailService;

  @Override
  public List<EmailDTO> getEmails() {
    return emailService.getEmails();
  }

  @Override
  public void saveAllEmails(List<EmailDTO> emails) {
    emailService.saveAllEmails(emails);
  }

  @Override
  public void saveAllApplicationEvents(List<EmailApplicationClassification> classifications) {
    jobService.saveAllApplicationEvents(classifications);
  }

  @Override
  public List<JobApplicationDTO> listJobs() {
    return jobService.listJobs();
  }

  @Override
  public List<ApplicationEventDTO> listApplications() {
    return jobService.listApplications();
  }


}
