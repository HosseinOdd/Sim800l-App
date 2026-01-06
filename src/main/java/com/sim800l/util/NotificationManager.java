package com.sim800l.util;

import java.io.IOException;

public class NotificationManager {
    
    public static void show(String title, String message) {
        // Sanitize inputs to prevent command injection
        if (title == null || title.isEmpty()) {
            title = "SIM800L Manager";
        }
        if (message == null || message.isEmpty()) {
            message = "";
        }
        
        // Limit lengths to prevent issues
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }
        
        // Remove control characters and shell special chars
        final String safeTitle = sanitize(title);
        final String safeMessage = sanitize(message);
        
        // Use system notify-send on Linux
        new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(
                    "notify-send",
                    "-i", "mail-message-new",
                    "-a", "SIM800L Manager",
                    "-u", "normal",
                    safeTitle,
                    safeMessage
                );
                // Disable shell interpretation
                builder.redirectErrorStream(true);
                builder.start();
            } catch (IOException e) {
                // Fallback to console output
                System.out.println("[NOTIFICATION] " + safeTitle + ": " + safeMessage);
            }
        }).start();
    }
    
    /**
     * Sanitize string to prevent command injection
     */
    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        // Remove dangerous characters but keep Unicode
        return input.replaceAll("[`$\\\\\"';|&<>]", "");
    }
}
