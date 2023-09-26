package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.app.user.UserRoles;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_CONFLICT;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired
    RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getDetails();
            // apply rate limits to all authenticated, non-subscribing users
            // TODO: clean this up to apply to only the API paths (no need to rate limit the app server)
            if (!userDetails.getAuthorities().contains(UserRoles.API_SUBSCRIBER_AUTHORITY)) {
                String username = userDetails.getUsername();
                Bucket bucket = rateLimiter.resolveBucket(username);
                log.trace("Checking rate limit...");
                if (bucket != null && !bucket.tryConsume(1)) {
                    log.debug("Rate limit exceeded");
                    response.setStatus(SC_CONFLICT); // 409
                    response.getWriter().write("Rate limit exceeded");
                    response.getWriter().flush();
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
