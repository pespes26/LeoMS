package org.leoms.admin.security;

import jakarta.servlet.http.HttpServletRequest;

final class ClientAddress {
    private ClientAddress() {}

    static String from(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if ("127.0.0.1".equals(remote) || "::1".equals(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].strip();
            }
        }
        return remote;
    }
}
