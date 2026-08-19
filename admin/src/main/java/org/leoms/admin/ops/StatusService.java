package org.leoms.admin.ops;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatusService {
    private final JdbcTemplate jdbc;

    public StatusService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            status.put("Database", "healthy");
            status.put("Accounts", jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class));
            status.put("Characters", jdbc.queryForObject("SELECT COUNT(*) FROM characters", Integer.class));
        } catch (RuntimeException e) {
            status.put("Database", "unavailable");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("maplestory", 8484), 1500);
            status.put("Login server", "healthy");
        } catch (IOException e) {
            status.put("Login server", "unavailable");
        }
        return status;
    }
}
