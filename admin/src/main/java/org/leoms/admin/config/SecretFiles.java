package org.leoms.admin.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SecretFiles {
    private SecretFiles() {}

    public static String readRequired(String property, String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(property + " must point to a secret file");
        }
        try {
            String value = Files.readString(Path.of(path)).strip();
            if (value.isEmpty()) {
                throw new IllegalStateException(property + " points to an empty secret file");
            }
            return value;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + property, e);
        }
    }
}
