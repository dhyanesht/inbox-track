package com.dino.inbox_track.dto;

import com.dino.inbox_track.db.EventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationEventDto(
    UUID id,
    OffsetDateTime eventDate,
    EventType eventType
) {}
