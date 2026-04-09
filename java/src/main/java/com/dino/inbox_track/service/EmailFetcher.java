package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.EmailDTO;
import java.util.List;

public interface EmailFetcher {
  List<String> fetchMessageIds(int daysBack);
  List<EmailDTO> fetchEmails(List<String> messageIds);
  List<EmailDTO> filterJobApplications(List<EmailDTO> emails);
}