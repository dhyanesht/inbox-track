package com.dino.inbox_track.db;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

  List<JobApplication> findByApplicationDateAfter(OffsetDateTime offsetDateTime);

  @Query("""
          SELECT j FROM JobApplication j
          LEFT JOIN FETCH j.events e
      """)
  List<JobApplication> findAllWithEvents();
}
