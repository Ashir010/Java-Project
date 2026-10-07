package com.internportal.model;

/** Where one week of a student's logbook stands. */
public enum WeekStatus {
    NOT_STARTED("NOT STARTED"),
    PENDING("PENDING"),
    SUBMITTED("SUBMITTED"),
    GRADED("GRADED");

    private final String label;

    WeekStatus(String label) {
        this.label = label;
    }

    /** The text shown in tables (the status colours are matched on this text). */
    public String getLabel() {
        return label;
    }
}
