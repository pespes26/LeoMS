package tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsAndTrimsRequiredSecret() throws IOException {
        Path secret = temporaryDirectory.resolve("password");
        Files.writeString(secret, "correct horse battery\n");

        assertEquals("correct horse battery", DatabaseConnection.readRequiredSecret(secret, "DB_PASSWORD_FILE"));
    }

    @Test
    void rejectsEmptySecret() throws IOException {
        Path secret = temporaryDirectory.resolve("empty");
        Files.writeString(secret, " \n");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> DatabaseConnection.readRequiredSecret(secret, "DB_PASSWORD_FILE"));
        assertTrue(error.getMessage().contains("empty secret"));
    }

    @Test
    void rejectsUnreadableSecret() {
        Path missing = temporaryDirectory.resolve("missing");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> DatabaseConnection.readRequiredSecret(missing, "DB_PASSWORD_FILE"));
        assertTrue(error.getMessage().contains("DB_PASSWORD_FILE"));
    }
}
