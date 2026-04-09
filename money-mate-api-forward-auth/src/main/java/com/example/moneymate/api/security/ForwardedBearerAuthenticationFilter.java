package com.example.moneymate.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ForwardedBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ForwardedBearerAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            log.info(
                "Incoming request {} {} authorizationHeader={}",
                request.getMethod(),
                request.getRequestURI(),
                previewAuthorizationHeader(authorizationHeader)
            );

            String token = extractBearerToken(authorizationHeader);
            if (token != null) {
                log.info(
                    "Accepted forwarded bearer token for {} {} tokenPreview={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    previewToken(token)
                );
                ForwardedAuthorization principal = new ForwardedAuthorization(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    token,
                    principal.authorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String header) {
        if (header == null) {
            return null;
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private String previewAuthorizationHeader(String header) {
        if (header == null || header.isBlank()) {
            return "<missing>";
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return header;
        }
        return "Bearer " + previewToken(header.substring("Bearer ".length()).trim());
    }

    private String previewToken(String token) {
        if (token == null || token.isBlank()) {
            return "<empty>";
        }
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
