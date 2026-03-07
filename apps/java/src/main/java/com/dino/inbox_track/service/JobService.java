package com.dino.inbox_track.service;

import com.dino.inbox_track.db.ApplicationEvent;
import com.dino.inbox_track.db.ApplicationStatus;
import com.dino.inbox_track.db.EventType;
import com.dino.inbox_track.db.JobApplication;
import com.dino.inbox_track.db.JobApplicationRepository;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class JobService {


    private final JobApplicationRepository jobApplicationRepository;

    public JobService(JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
    }

    public List<JobApplication> listJobs() {

        return jobApplicationRepository.findAll();
    }

    @Transactional
    public JobApplication saveJob(EmailApplicationClassification dto) {
        if (!dto.getIsJobApplication()) {
            return null; // Skip non-job emails
        }

        JobApplication job = JobApplication.builder()
                .userId(getCurrentUserId())
                .companyName(dto.getCompany())
                .position(dto.getPositionTitle())
                .status(mapStatus(dto.getApplicationStatus()))
                .applicationDate(OffsetDateTime.now())
                .lastUpdated(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .events(new ArrayList<>())
                .build();

        // Create email classification event
        ApplicationEvent event = ApplicationEvent.builder()
                .application(job)
                .eventType(EventType.email_received)
                .title(dto.getSubject())
                .description("Email classified as job application: " + dto.getCompany() + " - " + dto.getPositionTitle())
                .eventDate(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        job.getEvents().add(event);

        return jobApplicationRepository.save(job);
    }

    private ApplicationStatus mapStatus(String statusStr) {
        return switch (statusStr != null ? statusStr.toLowerCase() : "") {
            case "applied", "submitted" -> ApplicationStatus.applied;
            case "interview", "phone screen", "phone" -> ApplicationStatus.interview;
            case "offer" -> ApplicationStatus.offer;
            case "rejected" -> ApplicationStatus.rejected;
            default -> ApplicationStatus.applied;
        };
    }

    private UUID getCurrentUserId() {
        // Implement based on your authentication context
        // e.g., SecurityContextHolder.getContext().getAuthentication()
        return UUID.randomUUID(); // Placeholder
    }

}
