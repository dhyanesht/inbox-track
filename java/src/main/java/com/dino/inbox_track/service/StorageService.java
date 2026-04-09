package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.ApplicationEventDTO;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.dto.JobApplicationDTO;
import java.util.List;

public interface StorageService {
  List<EmailDTO> getEmails();
  void saveAllEmails(List<EmailDTO> emails);
  void saveAllApplicationEvents(List<EmailApplicationClassification> classifications);
  List<JobApplicationDTO> listJobs();
  List<ApplicationEventDTO> listApplications();
}
