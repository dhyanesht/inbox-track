package com.dino.inbox_track.service;

import com.dino.inbox_track.db.ApplicationEvent;
import com.dino.inbox_track.db.ApplicationEventRepository;
import com.dino.inbox_track.db.ApplicationStatus;
import com.dino.inbox_track.db.EventType;
import com.dino.inbox_track.db.JobApplication;
import com.dino.inbox_track.db.JobApplicationRepository;
import com.dino.inbox_track.dto.ApplicationEventDto;
import com.dino.inbox_track.dto.ApplicationEventMapper;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {


    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationEventRepository applicationEventRepository;



    public List<JobApplication> listJobs() {

        return jobApplicationRepository.findAll();
    }

    @Transactional
    public ApplicationEvent saveApplicationEvent(EmailApplicationClassification dto) {

        ApplicationEvent event = ApplicationEvent.builder()
            .eventType(EventType.email_received)
            .title(dto.getSubject())
            .description("Email classified as job application: " + dto.getCompany() + " - " + dto.getPositionTitle())
            .eventDate(OffsetDateTime.now())
            .createdAt(OffsetDateTime.now())
            .build();
        applicationEventRepository.save(event);
        return event;
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

    @Transactional
    public void saveAllApplicationEvents(List<EmailApplicationClassification> classifications) {
        List<ApplicationEvent> events = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        for (EmailApplicationClassification classification : classifications) {
            events.add(ApplicationEvent.builder()
                .eventType(EventType.email_received)
                .title(classification.getSubject())
                .description("Email classified as job application: " + classification.getCompany() + " - "
                    + classification.getPositionTitle())
                .eventDate(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build());
        }
        applicationEventRepository.saveAll(events);
    }

    public List<ApplicationEventDto> listApplications() {
        System.out.println(applicationEventRepository.findAll());
        return applicationEventRepository.findAll().stream().map(ApplicationEventMapper::toDto).toList();
    }
}
