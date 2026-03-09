package com.dino.inbox_track.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailApplicationClassification {

    String emailId;
    String subject;
    Boolean isJobApplication;
    String company;
    String positionTitle;
    String applicationStage;
    String applicationStatus;


}
