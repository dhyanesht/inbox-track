package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailDTO;
import java.util.List;

public interface EmailClassifier {
  List<EmailApplicationClassification> classifyApplications(List<EmailDTO> jobApplications);
}