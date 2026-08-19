package org.leoms.admin.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class RecentLogService {
    private static final int MAX_BYTES = 64 * 1024;
    private static final int MAX_LINES = 200;
    private static final List<String> ALLOWED = List.of("cosmic-log.log", "trades.log", "expeditions.log");
    private final Path directory;

    public RecentLogService(@Value("${leoms.logs.directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public List<String> allowedLogs() {
        return ALLOWED.stream().filter(name -> Files.isRegularFile(directory.resolve(name))).toList();
    }

    public String tail(String requested) {
        String name = ALLOWED.contains(requested) ? requested : "cosmic-log.log";
        Path file = directory.resolve(name).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) return "No log is available yet.";
        try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
            long start = Math.max(0, input.length() - MAX_BYTES);
            input.seek(start);
            byte[] bytes = new byte[(int) (input.length() - start)];
            input.readFully(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = text.split("\\R");
            int first = Math.max(0, lines.length - MAX_LINES);
            return String.join(System.lineSeparator(), List.of(lines).subList(first, lines.length));
        } catch (IOException e) {
            return "Unable to read the selected log.";
        }
    }
}
