package com.dino.inbox_track.service;

import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.EmailDTO;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncOrchestratorImpl implements SyncOrchestrator {

  private final EmailFetcher emailFetcher;
  private final EmailClassifier emailClassifier;
  private final StorageService storageService;
  private final CheckpointService checkpointService;

  // In-memory tracking of active syncs (for MVP - replace with proper store later)
  private final Map<String, SyncStatus> activeSyncs = new ConcurrentHashMap<>();
  private final Map<String, SyncResult> syncResults = new ConcurrentHashMap<>();

  @Async
  @Override
  public String startSync() {
    String syncId = UUID.randomUUID().toString();
    SyncStatus status = new SyncStatus(syncId, SyncStatus.Status.STARTED);
    activeSyncs.put(syncId, status);

    try {
      // Step 1: Fetch emails
      status.setStatus(SyncStatus.Status.FETCHING_EMAILS);
      List<String> messageIds = emailFetcher.fetchMessageIds(30); // Last 30 days

      // Filter out already processed emails using checkpoint
      Set<String> processedIds = checkpointService.getProcessedEmailIds();
      List<String> newMessageIds = messageIds.stream()
          .filter(id -> !processedIds.contains(id))
          .toList();

      status.setTotalEmails(newMessageIds.size());

      // Step 2: Fetch full email details in batches
      status.setStatus(SyncStatus.Status.FETCHING_EMAIL_DETAILS);
      List<EmailDTO> allEmails = new ArrayList<>();
      int batchSize = 10;
      for (int i = 0; i < newMessageIds.size(); i += batchSize) {
        List<String> batch = newMessageIds.subList(i, Math.min(i + batchSize, newMessageIds.size()));
        List<EmailDTO> emails = emailFetcher.fetchEmails(batch);
        allEmails.addAll(emails);
        status.setProcessedEmails(i + batch.size());
      }

      // Step 3: Filter job applications
      status.setStatus(SyncStatus.Status.FILTERING_JOB_APPLICATIONS);
      List<EmailDTO> jobApplications = emailFetcher.filterJobApplications(allEmails);
      status.setJobApplicationsFound(jobApplications.size());

      // Step 4: Save emails
      status.setStatus(SyncStatus.Status.SAVING_EMAILS);
      storageService.saveAllEmails(jobApplications);

      // Step 5: Classify applications
      status.setStatus(SyncStatus.Status.CLASSIFYING_APPLICATIONS);
      List<EmailApplicationClassification> classifications =
          emailClassifier.classifyApplications(jobApplications);

      // Step 6: Save application events
      status.setStatus(SyncStatus.Status.SAVING_APPLICATIONS);
      storageService.saveAllApplicationEvents(classifications);

      // Mark emails as processed (checkpoint)
      for (EmailDTO email : jobApplications) {
        checkpointService.markAsProcessed(email.getEmailId());
      }

      status.setStatus(SyncStatus.Status.COMPLETED);
      status.setCompletedAt(ZonedDateTime.now());

      // Store results
      SyncResult result = new SyncResult();
      result.setSyncId(syncId);
      result.setClassifications(classifications);
      result.setEmailsProcessed(jobApplications.size());
      syncResults.put(syncId, result);

    } catch (Exception e) {
      status.setStatus(SyncStatus.Status.FAILED);
      status.setErrorMessage(e.getMessage());
      // Log the error
    }

    return syncId;
  }

  @Override
  public SyncStatus getStatus(String syncId) {
    return activeSyncs.get(syncId);
  }

  @Override
  public SyncResult getResults(String syncId) {
    return syncResults.get(syncId);
  }
}
