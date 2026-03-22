package com.dino.inbox_track.controller;

import com.dino.inbox_track.db.JobApplication;
import com.dino.inbox_track.dto.ApplicationEventDto;
import com.dino.inbox_track.dto.EmailDTO;
import com.dino.inbox_track.service.EmailService;
import com.dino.inbox_track.service.JobService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")

public class JobController {

    private final JobService jobService;
    private final EmailService emailService;


    public JobController(JobService jobService, EmailService emailService) {
        this.jobService = jobService;
        this.emailService = emailService;
    }

    @GetMapping("/")
    public List<JobApplication> listJobs() {
        return jobService.listJobs();
    }

    @GetMapping("/applications")
    public List<ApplicationEventDto> listApplications() {
        return jobService.listApplications();
    }

    @GetMapping("/emails")
    public List<EmailDTO> getEmails() {
        return emailService.getEmails();
    }
}

