package com.dino.inbox_track.db;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailRepository extends JpaRepository<Email, UUID> {

  Optional<Email> findByMessageId(String messageId);

  @Query("SELECT e.messageId FROM Email e")
  Set<String> findAllMessageIds();

}
