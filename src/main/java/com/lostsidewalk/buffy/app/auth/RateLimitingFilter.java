package com.lostsidewalk.buffy.app.auth;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

import static jakarta.servlet.http.HttpServletResponse.SC_CONFLICT;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired
    RateLimiter rateLimiter;

    @Override
    protected final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getDetails();
            // TODO: clean this up to apply to only the API paths (no need to rate limit the internal-facing app server)
            String username = userDetails.getUsername();
            Bucket bucket = rateLimiter.resolveBucket(username);
            log.trace("Checking rate limit...");
            if (bucket != null && !bucket.tryConsume(1L)) {
                log.debug("Rate limit exceeded");
                response.setStatus(SC_CONFLICT); // 409
                PrintWriter writer = response.getWriter();
                writer.write("Rate limit exceeded");
                writer.flush();
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public final String toString() {
        return "RateLimitingFilter{" +
                "rateLimiter=" + rateLimiter +
                '}';
    }
}
