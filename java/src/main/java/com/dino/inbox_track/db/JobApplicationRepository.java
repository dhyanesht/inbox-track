package com.dino.inbox_track.db;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

  List<JobApplication> findByApplicationDateAfter(OffsetDateTime offsetDateTime);
}
