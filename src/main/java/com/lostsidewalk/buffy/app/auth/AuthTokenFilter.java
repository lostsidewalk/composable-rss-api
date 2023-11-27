package com.lostsidewalk.buffy.app.auth;

import com.google.common.collect.ImmutableSet;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.ApiKeyException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.auth.OptionsAuthHandler.MissingOptionsHeaderException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Stream;

import static com.lostsidewalk.buffy.app.audit.ErrorLogService.logDataAccessException;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Slf4j
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    OptionsAuthHandler optionsAuthHandler;

    @Autowired
    SingleUserModeProcessor singleUserModeProcessor;

    @Value("${comprss.singleUserMode:false}")
    boolean singleUserMode;

    @Autowired
    CurrentUserAuthHandler currentUserAuthHandler;

    @Autowired
    PasswordUpdateAuthHandler passwordUpdateAuthHandler;

    @Autowired
    ApiAuthHandler apiAuthHandler;

    @Autowired
    ApplicationAuthHandler applicationAuthHandler;

    @PostConstruct
    void postConstruct() {
        log.info("Auth token filter initializing, singleUserMode={}", singleUserMode);
    }

    @Override
    protected final void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = getPath(request);
        if (shouldApplyFilter(requestPath)) {
            //
            // the authentication filtering logic works as follows:
            //
            // /currentuser is called by the FE on initialization; if a valid auth refresh cookie is present, this will
            // update the refresh cookie just received and pass through to the AuthenticationController to fetch a
            // short-lived authentication token for follow-on requests
            //
            // all other requests must contain an authorization header with an auth token fetch from /currentuser
            //
            // there is special handling for password updates
            //
            String method = request.getMethod();
            StringBuffer requestURL = request.getRequestURL();
            try {
                if (StringUtils.equals(method, "OPTIONS")) {
                    OptionsAuthHandler.processRequest(request);
                } else if (StringUtils.equals(requestPath, "/currentuser")) {
                    if (singleUserMode) {
                        singleUserModeProcessor.setupLocalSession();
                    } else {
                        currentUserAuthHandler.processCurrentUser(request, response);
                    }
                } else if (startsWith(requestPath, "/pw_update")) {
                    //
                    // pw_reset->POST => password reset init call (no filter)
                    // pw_reset->GET => password reset callback (continuation, no filter)
                    // pw_update->PUT => password update call (special filter)
                    //
                    passwordUpdateAuthHandler.processPasswordUpdate(request);
                } else {
                    String apiKey = request.getHeader(API_KEY_HEADER_NAME);
                    if (isNotBlank(apiKey) && !singleUserMode) {
                        String apiSecret = request.getHeader(API_SECRET_HEADER_NAME);
                        String r = requestURL.toString();
                        apiAuthHandler.processApiRequest(apiKey, apiSecret, r, method, response);
                    } else if (isNotBlank(apiKey) && singleUserMode) {
                        singleUserModeProcessor.setupApiSession();
                    } else if (isBlank(apiKey) && !singleUserMode) {
                        applicationAuthHandler.processAllOthers(request, response);
                    } else if (isBlank(apiKey) && singleUserMode) {
                        singleUserModeProcessor.setupLocalSession();
                    }
                }
            } catch (MissingOptionsHeaderException e) {
                log.error("Invalid OPTIONS call for requestUrl={}, request header names: {}", requestURL, e.headerNames);
            } catch (TokenValidationException | UsernameNotFoundException ignored) {
                // ignore
            } catch (AuthClaimException | ApiKeyException e) {
                String rootCauseMessage = getRootCauseMessage(e);
                log.error("Cannot set user authentication for requestUrl={}, requestMethod={}, due to: {}", requestURL, method, rootCauseMessage);
            } catch (DataAccessException e) {
                logDataAccessException("sys", new Date(), e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static boolean shouldApplyFilter(String requestPath) {
        return !isOpenServletPath(requestPath);
    }

    private static String getPath(HttpServletRequest request) {
        return request.getServletPath();
    }

    private static final ImmutableSet<String> OPEN_PATHS = ImmutableSet.of(
            "/authenticate",
            "/v3/api-docs"
    );

    private static final ImmutableSet<String> OPEN_PATH_PREFIXES = ImmutableSet.of(
            "/pw_reset",
            "/register",
            "/verify",
            "/stripe",
            "/proxy/unsecured"
    );

    private static boolean isOpenServletPath(String servletPath) {
        Stream<String> stream = OPEN_PATH_PREFIXES.stream();
        return OPEN_PATHS.contains(servletPath) || stream.anyMatch(servletPath::startsWith);
    }

    //
    //
    //

    public static final String API_KEY_HEADER_NAME = "X-ComposableRSS-API-Key";

    public static final String API_SECRET_HEADER_NAME = "X-ComposableRSS-API-Secret";

    @Override
    public final String toString() {
        return "AuthTokenFilter{" +
                "optionsAuthHandler=" + optionsAuthHandler +
                ", currentUserAuthHandler=" + currentUserAuthHandler +
                ", passwordUpdateAuthHandler=" + passwordUpdateAuthHandler +
                ", apiAuthHandler=" + apiAuthHandler +
                ", applicationAuthHandler=" + applicationAuthHandler +
                '}';
    }
}
