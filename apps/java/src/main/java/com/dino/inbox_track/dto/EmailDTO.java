package com.dino.inbox_track.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Date;

@Data
@ToString
@Builder
@AllArgsConstructor
public class EmailDTO {

    String emailId;
    String from;
    LocalDate date;
    String subject;
    String message;

}

