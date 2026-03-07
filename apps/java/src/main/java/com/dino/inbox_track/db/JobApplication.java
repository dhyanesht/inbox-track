package com.dino.inbox_track.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_applications", schema = "job_track")
@Builder
@Data
public class JobApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String position;

    @Column(name = "job_url")
    private String jobUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.applied;

    private String location;

    @Column(name = "salary_range")
    private String salaryRange;

    @Column(name = "application_date", nullable = false)
    private OffsetDateTime applicationDate;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "email_thread_id")
    private String emailThreadId;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.medium;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationEvent> events;

}

