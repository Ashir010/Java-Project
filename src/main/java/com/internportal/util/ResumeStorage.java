package com.internportal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Saves, finds and deletes uploaded resumes in uploads/resumes (next to where the app runs).
 * Stored files get a generated name, so the name the user's file had is never used on disk.
 */
public final class ResumeStorage {

    public static final long MAX_BYTES = 5L * 1024 * 1024;   // 5 MB

    private static final String STORED_PREFIX = "uploads/resumes/";
    private static final Path BASE_DIR = Paths.get("uploads", "resumes");

    private ResumeStorage() { }

    /**
     * Validates the file and copies it into the uploads folder.
     * Returns the stored path to keep in the database.
     * Throws IllegalArgumentException (message safe to show) if the file is not acceptable.
     */
    public static String save(int studentId, File source) throws IOException {
        String name = source.getName().toLowerCase(Locale.ROOT);
        String extension;
        if (name.endsWith(".pdf")) {
            extension = "pdf";
        } else if (name.endsWith(".docx")) {
            extension = "docx";
        } else {
            throw new IllegalArgumentException("Please choose a PDF or DOCX file.");
        }

        long size = source.length();
        if (size <= 0) {
            throw new IllegalArgumentException("The selected file is empty.");
        }
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("The file is larger than 5 MB.");
        }
        if (!hasValidSignature(source, extension)) {
            throw new IllegalArgumentException(
                    "This does not look like a real " + extension.toUpperCase(Locale.ROOT) + " file.");
        }

        Files.createDirectories(BASE_DIR);
        String storedName = "student" + studentId + "_" + System.currentTimeMillis() + "." + extension;
        Files.copy(source.toPath(), BASE_DIR.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        return STORED_PREFIX + storedName;
    }

    /** The real file for a stored path, or null if the path is empty or points outside uploads/resumes. */
    public static Path resolve(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return null;
        }
        Path base = BASE_DIR.toAbsolutePath().normalize();
        Path file = Paths.get(storedPath).toAbsolutePath().normalize();
        return file.startsWith(base) ? file : null;
    }

    /** Best-effort delete; a missing file is not an error. */
    public static void delete(String storedPath) {
        Path file = resolve(storedPath);
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // nothing useful to do if the file cannot be removed
            }
        }
    }

    /** "PDF" or "DOCX", for display. */
    public static String typeLabel(String storedPath) {
        return storedPath != null && storedPath.toLowerCase(Locale.ROOT).endsWith(".pdf") ? "PDF" : "DOCX";
    }

    /** Checks the first bytes: PDF files start with %PDF, DOCX files are zip archives starting with PK. */
    private static boolean hasValidSignature(File file, String extension) throws IOException {
        byte[] header = new byte[4];
        int read;
        try (InputStream in = new FileInputStream(file)) {
            read = in.read(header);
        }
        if (read < 4) {
            return false;
        }
        if (extension.equals("pdf")) {
            return header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
        }
        return header[0] == 'P' && header[1] == 'K';
    }
}
