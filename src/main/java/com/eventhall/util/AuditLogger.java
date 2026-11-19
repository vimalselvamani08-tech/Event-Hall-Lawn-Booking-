package com.eventhall.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class AuditLogger {
    private static final Path LOG_FILE = Path.of("logs", "audit.log");

    private AuditLogger() { }

    public static synchronized void log(String action, String detail) {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, LocalDateTime.now() + " [" + action + "] " + detail
                    + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("Unable to write audit log: " + exception.getMessage());
        }
    }
}