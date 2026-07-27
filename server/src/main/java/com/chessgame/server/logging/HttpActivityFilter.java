package com.chessgame.server.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs the REST side of the conversation.
 *
 * Login, registration and the move-history snapshot are plain HTTP and never
 * touch the STOMP channels, so the interceptor cannot see them.
 *
 * Request bodies are deliberately not read here. Doing so would require
 * wrapping and caching the stream, and the only bodies worth reading are the
 * auth ones - which carry passwords. Method, path and status describe the
 * activity without ever holding a credential in memory.
 */
@Component
public final class HttpActivityFilter extends OncePerRequestFilter {

    private static final String LOGGED_PREFIX = "/api/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                ActivityLog.http(request.getMethod(), request.getRequestURI(),
                        response.getStatus(), System.currentTimeMillis() - startedAt);
            } catch (RuntimeException ignored) {
                // A failing log line must never turn into a failed request.
            }
        }
    }

    /**
     * Sprites, stylesheets and the board image would drown the log without
     * saying anything about what the players actually did.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(LOGGED_PREFIX);
    }
}
