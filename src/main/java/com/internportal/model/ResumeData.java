package com.internportal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the resume parser found in a resume. phone is null when no number was found. */
public class ResumeData {

    private final String phone;
    private final List<String> skills;

    public ResumeData(String phone, List<String> skills) {
        this.phone = phone;
        this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getSkills() {
        return skills;
    }

    public boolean isEmpty() {
        return phone == null && skills.isEmpty();
    }
}
