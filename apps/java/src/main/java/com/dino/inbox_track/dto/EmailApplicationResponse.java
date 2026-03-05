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
public class EmailApplicationResponse {

    String emailId;
    String subject;
    Boolean isJobApplication;

}
