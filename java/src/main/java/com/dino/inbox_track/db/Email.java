package com.dino.inbox_track.db;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "emails", schema = "job_track")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue
    private UUID id;
    private String messageId;
    @Column(name = "from_email") // from is reserved keyword
    private String from;
    private LocalDate date;
    private String subject;
    private String body;
    @OneToOne
    private JobApplication application;
}
