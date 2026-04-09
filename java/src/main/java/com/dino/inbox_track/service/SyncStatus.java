package com.dino.inbox_track.service;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncStatus {

  private String syncId;
  private Status status;
  private int totalEmails;
  private int processedEmails;
  private int jobApplicationsFound;
  private ZonedDateTime startedAt;
  private ZonedDateTime completedAt;
  private String errorMessage;

  public SyncStatus(String syncId, Status status) {
    this.syncId = syncId;
    this.status = status;
  }

  public enum Status {
    STARTED, FETCHING_EMAILS, FETCHING_EMAIL_DETAILS,
    FILTERING_JOB_APPLICATIONS, SAVING_EMAILS,
    CLASSIFYING_APPLICATIONS, SAVING_APPLICATIONS,
    COMPLETED, FAILED
  }

}
