package com.dino.inbox_track.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailApplicationClassification {

    private String emailId;
    private String from;
    private String to;
    private String subject;
    private Boolean isJobApplication;

    private String company;
    private String country;
    private String positionTitle;
    private String positionLocation;
    private String salary;
    private String applicationStage;
    private String jobId;
    private String applicationStatus;
    private String nextStep;
    private String applicationDate;
    private String deadline;
    private String remotePolicy;
    private String employmentType;
    private String experienceLevel;
    private String hiringManager;
}
