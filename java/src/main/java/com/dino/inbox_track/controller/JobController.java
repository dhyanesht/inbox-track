package com.dino.inbox_track.controller;

import com.dino.inbox_track.db.JobApplication;
import com.dino.inbox_track.service.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/job")

public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/")
    public List<JobApplication> listJobs() {
        return jobService.listJobs();
    }
}
