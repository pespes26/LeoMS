package org.leoms.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class LoginThrottleFilter extends OncePerRequestFilter {
    private final LoginAttemptService attempts;

    public LoginThrottleFilter(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath())
                && attempts.isBlocked(ClientAddress.from(request))) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many login attempts. Try again later.");
            return;
        }
        chain.doFilter(request, response);
    }
}
