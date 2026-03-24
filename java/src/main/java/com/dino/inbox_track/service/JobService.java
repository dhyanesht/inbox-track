package com.dino.inbox_track.service;

import com.dino.inbox_track.db.ApplicationEvent;
import com.dino.inbox_track.db.ApplicationEventRepository;
import com.dino.inbox_track.db.ApplicationStatus;
import com.dino.inbox_track.db.Email;
import com.dino.inbox_track.db.EmailRepository;
import com.dino.inbox_track.db.EventType;
import com.dino.inbox_track.db.JobApplication;
import com.dino.inbox_track.db.JobApplicationRepository;
import com.dino.inbox_track.dto.ApplicationEventDTO;
import com.dino.inbox_track.dto.ApplicationEventMapper;
import com.dino.inbox_track.dto.EmailApplicationClassification;
import com.dino.inbox_track.dto.JobApplicationDTO;
import com.dino.inbox_track.dto.JobApplicationMapper;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {


    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationEventRepository applicationEventRepository;
    private final EmailRepository emailRepository;
    // In-memory cache: key = company|position, value = JobApplication
    private final Map<String, JobApplication> jobCache = new ConcurrentHashMap<>();
    private final JobApplicationMapper jobApplicationMapper;


    public List<JobApplicationDTO> listJobs() {

        return jobApplicationRepository.findAllWithEvents()
            .stream()
            .map(jobApplicationMapper::toDto)
            .toList();
    }

    /**
     * Refresh the in-memory cache with all jobs from last 6 months
     */
    @Scheduled(fixedDelay = 60_000) // every 60 seconds or as needed
    public void refreshCache() {
        OffsetDateTime sixMonthsAgo = OffsetDateTime.now().minusMonths(6);
        List<JobApplication> recentJobs = jobApplicationRepository.findByApplicationDateAfter(sixMonthsAgo);

        Map<String, JobApplication> updatedCache = recentJobs.stream()
            .collect(Collectors.toMap(
                j -> j.getCompanyName() + "|" + j.getPosition(),
                j -> j
            ));
        jobCache.clear();
        jobCache.putAll(updatedCache);
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


    @Transactional
    public void saveAllApplicationEvents(List<EmailApplicationClassification> classifications) {
        List<ApplicationEvent> events = new ArrayList<>();

        for (EmailApplicationClassification classification : classifications) {
            String key = classification.getCompany() + "|" + classification.getPositionTitle();
            JobApplication jobApplication = jobCache.computeIfAbsent(key, j -> {
                JobApplication newJob = JobApplication.builder()
                    .companyName(classification.getCompany() != null ? classification.getCompany() : "null")
                    .position(classification.getPositionTitle() != null ? classification.getPositionTitle() : "null")
                    .status(EventType.fromString(classification.getApplicationStage()).toApplicationStatus())
                    .notes(classification.getNotes())
                    .applicationDate(OffsetDateTime.now())
                    .lastUpdated(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .build();
                // Save to DB
                return jobApplicationRepository.save(newJob);
            });
            Optional<Email> emailOptional = emailRepository.findByMessageId(classification.getEmailId());
            events.add(ApplicationEvent.builder()
                .application(jobApplication)
                .eventType(EventType.fromString(classification.getApplicationStage()))
                .eventDate(OffsetDateTime.now())
                .email(emailOptional.orElseThrow())
                .build());
        }
        applicationEventRepository.saveAll(events);
    }

    public List<ApplicationEventDTO> listApplications() {
        return applicationEventRepository.findAll().stream().map(ApplicationEventMapper::toDto).toList();
    }
}
