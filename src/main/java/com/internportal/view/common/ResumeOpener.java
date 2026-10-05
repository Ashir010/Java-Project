package com.internportal.view.common;

import com.internportal.util.ResumeStorage;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opens a stored resume in the computer's default PDF or Word program. */
public final class ResumeOpener {

    private ResumeOpener() { }

    public static void open(Component parent, String storedPath) {
        Path file = ResumeStorage.resolve(storedPath);
        if (file == null || !Files.exists(file)) {
            error(parent, "The resume file could not be found.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            error(parent, "Files cannot be opened on this system.\nThe resume is saved at:\n" + file);
            return;
        }
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | RuntimeException e) {
            error(parent, "Could not open the resume: " + e.getMessage());
        }
    }

    private static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Resume", JOptionPane.ERROR_MESSAGE);
    }
}
