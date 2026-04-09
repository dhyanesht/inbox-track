package com.dino.inbox_track.service;

import com.dino.inbox_track.db.Email;
import com.dino.inbox_track.db.EmailRepository;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckpointServiceImpl implements CheckpointService {

  private final EmailRepository emailRepository;

  @Override
  public Set<String> getProcessedEmailIds() {
    return emailRepository.findAllMessageIds();
  }

  @Override
  public void markAsProcessed(String emailId) {
    // Email is already marked as processed when saved via EmailService
    // This method exists for interface completeness
  }

  @Override
  public void clearCheckpoint() {
    // Optional: clear all checkpoints if needed
  }
}
