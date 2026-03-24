package com.dino.inbox_track.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationDTO {

  private UUID id;
  private String companyName;
  private String position;
  private String status;
  private List<ApplicationEventDTO> events;
}
