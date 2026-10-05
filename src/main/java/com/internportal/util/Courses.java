package com.internportal.util;

import java.util.Arrays;
import java.util.List;

/** The one list of courses used by the Register and Profile screens. */
public final class Courses {

    public static final String[] ALL = {
            "B.Tech CSE", "B.Tech IT", "B.Tech ECE", "B.Tech (Other)",
            "BCA", "MCA", "B.Sc (CS/IT)", "M.Tech", "Other"
    };

    private static final List<String> LIST = Arrays.asList(ALL);

    private Courses() { }

    public static boolean isValid(String course) {
        return course != null && LIST.contains(course);
    }
}
