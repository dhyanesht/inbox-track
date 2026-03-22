package com.dino.inbox_track.dto;

import com.dino.inbox_track.db.Email;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {

    String emailId;
    String from;
    LocalDate date;
    String subject;
    String message;

    public static EmailDTO toEmailDTO(Email email) {
        return builder()
            .emailId(email.getFrom())
            .from(email.getFrom())
            .date(email.getDate())
            .subject(email.getSubject())
            .message(email.getBody())
            .build();

    }
}

