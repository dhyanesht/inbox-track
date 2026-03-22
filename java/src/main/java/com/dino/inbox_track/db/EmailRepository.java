package com.dino.inbox_track.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, UUID> {

  Optional<Email> findByMessageId(String messageId);
}
