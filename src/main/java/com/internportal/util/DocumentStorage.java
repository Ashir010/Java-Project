package com.internportal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Where the generated PDFs are kept: uploads/documents, next to where the app runs. */
public final class DocumentStorage {

    private static final String STORED_PREFIX = "uploads/documents/";
    private static final Path BASE_DIR = Paths.get("uploads", "documents");

    private DocumentStorage() { }

    /** The path to keep in the database. The reference number only contains letters, digits and hyphens. */
    public static String storedPathFor(String referenceNo) {
        return STORED_PREFIX + referenceNo + ".pdf";
    }

    public static void ensureFolder() throws IOException {
        Files.createDirectories(BASE_DIR);
    }

    /** The real file for a stored path, or null if the path is empty or points outside uploads/documents. */
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
}
