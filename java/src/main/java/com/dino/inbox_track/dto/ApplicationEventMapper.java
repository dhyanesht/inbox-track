package com.dino.inbox_track.dto;

import com.dino.inbox_track.db.ApplicationEvent;

public class ApplicationEventMapper {

  public static ApplicationEventDto toDto(ApplicationEvent event) {
    return new ApplicationEventDto(
        event.getId(),
        event.getEventDate(),
        event.getEventType()
    );
  }
}
