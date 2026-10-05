package com.internportal.model;

import java.time.LocalDateTime;

/** One step in an application's timeline. changedOn can be null if the date was not recorded. */
public class StatusChange {

    private final ApplicationStatus status;
    private final LocalDateTime changedOn;

    public StatusChange(ApplicationStatus status, LocalDateTime changedOn) {
        this.status = status;
        this.changedOn = changedOn;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getChangedOn() {
        return changedOn;
    }
}
