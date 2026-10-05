package com.internportal.util;

import com.internportal.model.ResumeData;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automated resume parser. Tika only extracts the text; the phone number and skills are then
 * found with a regular expression and a keyword list.
 */
public final class ResumeParser {

    private static final int MAX_CHARS = 200_000;

    // Indian mobile number: optional +91, then 10 digits starting with 6-9 (also "98765 43210")
    private static final Pattern PHONE =
            Pattern.compile("(?<!\\d)(?:\\+?91[\\s-]?)?([6-9]\\d{4}[\\s-]?\\d{5})(?!\\d)");

    private static final String[] SKILLS = {
            "Java", "Python", "C++", "C#", "JavaScript", "TypeScript", "SQL", "MySQL", "PostgreSQL",
            "MongoDB", "HTML", "CSS", "Bootstrap", "React", "Angular", "Vue.js", "Node.js",
            "Spring Boot", "Spring MVC", "Hibernate", "JDBC", "Servlets", "JSP", "Swing", "Maven",
            "Git", "GitHub", "Docker", "Kubernetes", "AWS", "Azure", "Linux", "Android", "Kotlin",
            "PHP", "Flutter", "REST API", "Data Structures", "Algorithms", "Machine Learning",
            "Deep Learning", "Pandas", "NumPy", "TensorFlow", "MS Excel", "Power BI", "Tableau",
            "Figma", "Selenium", "JUnit", "Jira", "OOP"
    };

    // Words that are also ordinary English, so they only count when the capital letters match
    private static final Set<String> CASE_SENSITIVE = new HashSet<>(Arrays.asList("React", "Swing"));

    private static final Map<String, Pattern> SKILL_PATTERNS = new LinkedHashMap<>();

    static {
        for (String skill : SKILLS) {
            int flags = CASE_SENSITIVE.contains(skill) ? 0 : Pattern.CASE_INSENSITIVE;
            // The skill must not be glued to other letters or digits ("Java" must not match "JavaScript")
            SKILL_PATTERNS.put(skill, Pattern.compile(
                    "(?<![A-Za-z0-9])" + Pattern.quote(skill) + "(?![A-Za-z0-9])", flags));
        }
    }

    private ResumeParser() { }

    /** Reads a PDF or DOCX file with Tika, then extracts the data. */
    public static ResumeData fromFile(Path file) throws IOException, TikaException {
        Tika tika = new Tika();
        tika.setMaxStringLength(MAX_CHARS);
        return fromText(tika.parseToString(file.toFile()));
    }

    /** Extracts the data from plain text. */
    public static ResumeData fromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ResumeData(null, new ArrayList<>());
        }
        String flat = text.replaceAll("\\s+", " ");   // line breaks and double spaces become one space
        return new ResumeData(findPhone(flat), findSkills(flat));
    }

    private static String findPhone(String text) {
        Matcher matcher = PHONE.matcher(text);
        if (matcher.find()) {
            String digits = matcher.group(1).replaceAll("\\D", "");
            if (Validator.isValidPhone(digits)) {
                return digits;
            }
        }
        return null;
    }

    private static List<String> findSkills(String text) {
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, Pattern> entry : SKILL_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                found.add(entry.getKey());
            }
        }
        return found;
    }
}
