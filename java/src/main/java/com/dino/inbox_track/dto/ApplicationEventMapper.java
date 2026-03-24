package com.dino.inbox_track.dto;

import com.dino.inbox_track.db.ApplicationEvent;

public class ApplicationEventMapper {

  private ApplicationEventMapper() {

  }

  public static ApplicationEventDTO toDto(ApplicationEvent event) {
    return new ApplicationEventDTO(
        event.getId(),
        event.getEventDate(),
        event.getEventType()
    );
  }
}
