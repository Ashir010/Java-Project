package com.internportal.view.common;

import com.internportal.model.IssuedDocument;
import com.internportal.util.DocumentStorage;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Open a document in the PDF viewer, or save a copy somewhere else. Shared by the student and admin screens. */
public final class DocumentActions {

    private DocumentActions() { }

    public static void open(Component parent, IssuedDocument doc) {
        Path file = existingFile(parent, doc);
        if (file == null) {
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            error(parent, "Files cannot be opened on this system.\nThe document is saved at:\n" + file);
            return;
        }
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | RuntimeException e) {
            error(parent, "Could not open the document: " + e.getMessage());
        }
    }

    public static void saveCopy(Component parent, IssuedDocument doc) {
        Path source = existingFile(parent, doc);
        if (source == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save a copy of " + doc.getReferenceNo());
        chooser.setFileFilter(new FileNameExtensionFilter("PDF document", "pdf"));
        chooser.setSelectedFile(new File(doc.getReferenceNo() + ".pdf"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getPath() + ".pdf");
        }
        if (target.exists() && JOptionPane.showConfirmDialog(parent,
                target.getName() + " already exists. Replace it?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(parent, "Saved to:\n" + target.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            error(parent, "Could not save the copy: " + e.getMessage());
        }
    }

    private static Path existingFile(Component parent, IssuedDocument doc) {
        Path file = DocumentStorage.resolve(doc.getFilePath());
        if (file == null || !Files.exists(file)) {
            error(parent, "The PDF file could not be found. The admin can delete this entry and issue it again.");
            return null;
        }
        return file;
    }

    private static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Document", JOptionPane.ERROR_MESSAGE);
    }
}
