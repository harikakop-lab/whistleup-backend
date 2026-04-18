package com.whistleup.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminPortalApiKeyFilter extends OncePerRequestFilter {

    private static final String ADMIN_PORTAL_PATH_PREFIX = "/whistleup/admin-portal/";
    private static final String HEADER_NAME = "X-Admin-Portal-Key";

    @Value("${app.admin-portal.api-key:}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith(ADMIN_PORTAL_PATH_PREFIX) || !StringUtils.hasText(configuredApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader(HEADER_NAME);
        if (configuredApiKey.equals(requestApiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Invalid admin portal API key\"}");
    }
}
