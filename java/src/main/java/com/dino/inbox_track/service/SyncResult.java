package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.EmailApplicationClassification;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SyncResult {
  private String syncId;
  private List<EmailApplicationClassification> classifications;
  private int emailsProcessed;
  private ZonedDateTime completedAt;

  // Getters and setters
}