package com.internportal.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** The dates an internship runs. Shown as "12 Nov 2026 to 03 Feb 2027" and sorted by start date. */
public class InternshipPeriod implements Comparable<InternshipPeriod> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final LocalDate start;
    private final LocalDate end;

    public InternshipPeriod(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    @Override
    public int compareTo(InternshipPeriod other) {
        int byStart = start.compareTo(other.start);
        return byStart != 0 ? byStart : end.compareTo(other.end);
    }

    @Override
    public String toString() {
        return start.format(FORMAT) + " to " + end.format(FORMAT);
    }
}
