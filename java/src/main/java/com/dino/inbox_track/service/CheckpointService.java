package com.dino.inbox_track.service;

import java.util.Set;

public interface CheckpointService {
  Set<String> getProcessedEmailIds();
  void markAsProcessed(String emailId);
  void clearCheckpoint();
}