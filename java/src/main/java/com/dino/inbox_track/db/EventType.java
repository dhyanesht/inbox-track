package com.dino.inbox_track.db;

public enum EventType {
    RECRUITER_REACHOUT,
    APPLIED,
    EMAIL_RECEIVED,
    INTERVIEW_SCHEDULED,
    FOLLOW_UP,
    STATUS_CHANGE,
    NOTE_ADDED,
    REVIEW,
    SELECTED,
    OFFER_ISSUED,
    REJECTED,
    RESCHEDULED;

    /**
     * Maps a free-form string (from LLM or elsewhere) to an EventType enum. Returns NOTE_ADDED as a safe fallback.
     */
    public static EventType fromString(String stage) {
        if (stage == null) {
            return NOTE_ADDED;
        }

        switch (stage.trim().toLowerCase()) {
            case "applied", "application submitted":
                return APPLIED;

            case "recruiter reachout":
                return RECRUITER_REACHOUT;

            case "email received":
                return EMAIL_RECEIVED;

            case "interview invitation", "interview scheduled":
                return INTERVIEW_SCHEDULED;

            case "follow up":
                return FOLLOW_UP;

            case "status change":
                return STATUS_CHANGE;

            case "review", "under review":
                return REVIEW;

            case "selected":
                return SELECTED;

            case "offer", "offer issued":
                return OFFER_ISSUED;

            case "rejected":
                return REJECTED;

            case "rescheduled", "re_scheduled":
                return RESCHEDULED;

            default:
                return NOTE_ADDED; // fallback for unknown stages
        }
    }

    /**
     * Maps each EventType to a corresponding ApplicationStatus. If an event doesn't change status, map it to the most relevant
     * current status.
     */
    public ApplicationStatus toApplicationStatus() {
        return switch (this) {
            case RECRUITER_REACHOUT -> ApplicationStatus.received;
            case APPLIED -> ApplicationStatus.applied;
            case EMAIL_RECEIVED -> ApplicationStatus.applied;       // Still in applied stage
            case INTERVIEW_SCHEDULED -> ApplicationStatus.interview;
            case FOLLOW_UP -> ApplicationStatus.interview;          // Keep in interview stage
            case STATUS_CHANGE -> ApplicationStatus.applied;        // generic fallback
            case NOTE_ADDED -> ApplicationStatus.applied;           // generic fallback
            case REVIEW -> ApplicationStatus.screening;             // under review = screening
            case SELECTED -> ApplicationStatus.offer;               // selected usually precedes offer
            case OFFER_ISSUED -> ApplicationStatus.offer;
            case REJECTED -> ApplicationStatus.rejected;
            case RESCHEDULED -> ApplicationStatus.interview;        // keep in interview stage
        };
    }
}
