package com.dino.inbox_track.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
public class EmailApplication {

    String emailId;
    String subject;
    String message;


}

