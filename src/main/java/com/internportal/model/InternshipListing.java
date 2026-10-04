package com.internportal.model;

/** An internship as a student sees it: the internship plus whether this student already applied. */
public class InternshipListing {

    private final Internship internship;
    private final boolean applied;

    public InternshipListing(Internship internship, boolean applied) {
        this.internship = internship;
        this.applied = applied;
    }

    public Internship getInternship() {
        return internship;
    }

    public boolean isApplied() {
        return applied;
    }
}
