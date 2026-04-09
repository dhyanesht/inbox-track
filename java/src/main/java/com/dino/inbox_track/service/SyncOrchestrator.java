package com.dino.inbox_track.service;

public interface SyncOrchestrator {
  String startSync(); // Returns syncId
  SyncStatus getStatus(String syncId);
  SyncResult getResults(String syncId);
}
