package com.dino.inbox_track.controller;

import com.dino.inbox_track.service.SyncOrchestrator;
import com.dino.inbox_track.service.SyncResult;
import com.dino.inbox_track.service.SyncStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

  private final SyncOrchestrator syncOrchestrator;

  @PostMapping("/start")
  public ResponseEntity<String> startSync() {
    String syncId = syncOrchestrator.startSync();
    return ResponseEntity.accepted().body(syncId);
  }

  @GetMapping("/status/{syncId}")
  public ResponseEntity<SyncStatus> getStatus(@PathVariable String syncId) {
    SyncStatus status = syncOrchestrator.getStatus(syncId);
    if (status == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(status);
  }

  @GetMapping("/results/{syncId}")
  public ResponseEntity<SyncResult> getResults(@PathVariable String syncId) {
    SyncResult result = syncOrchestrator.getResults(syncId);
    if (result == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }
}

