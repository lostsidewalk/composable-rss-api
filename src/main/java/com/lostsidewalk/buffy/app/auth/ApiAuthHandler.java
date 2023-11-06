package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.ApiKeyException;
import com.lostsidewalk.buffy.auth.ApiKey;
import com.lostsidewalk.buffy.auth.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
class ApiAuthHandler {

    @Autowired
    AuthService authService;

    @Autowired
    ApiUserService userService;

    final void processApiRequest(String apiKey, String apiSecret, String requestUrl, String requestMethod, @SuppressWarnings("unused") HttpServletResponse response)
            throws DataAccessException, ApiKeyException {
        if (isAnyBlank(apiKey, apiSecret)) {
            throw new ApiKeyException("Unable to locate API key or API secret headers");
        }
        User user = authService.findUserByApiKey(apiKey);
        if (user == null) {
            throw new ApiKeyException("Unable to locate API key by UUID");
        }
        String username = user.getUsername();
        ApiKey apiKeyObj = authService.requireApiKey(username);
        validateApiKey(apiKeyObj, apiKey, apiSecret);
        UserDetails userDetails = userService.loadUserByUsername(username);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        WebAuthenticationToken authToken = new WebAuthenticationToken(userDetails, apiKey, authorities);
        authToken.setDetails(userDetails);
        //
        // !! ACHTUNG !! POINT OF NO RETURN !!
        //
        SecurityContext context = getContext();
        context.setAuthentication(authToken);
        //
        // !! YOU'VE DONE IT NOW !!
        //
        log.debug("Logged in username={} via API header auth for requestUrl={}, requestMethod={}", username, requestUrl, requestMethod);
    }

    private static void validateApiKey(ApiKey apiKeyObj, String expectedKey, String expectedSecret) throws ApiKeyException {
        // ensure that the key UUIDs match
        String actual = apiKeyObj.getApiKey();
        if (!StringUtils.equals(expectedKey, actual)) {
            throw new ApiKeyException("API key mismatch");
        }
        // ensure that the secret values match
        String actualSecret = apiKeyObj.getApiSecret();
        if (!StringUtils.equals(expectedSecret, actualSecret)) {
            throw new ApiKeyException("API secret mismatch");
        }
    }

    @Override
    public final String toString() {
        return "ApiAuthHandler{" +
                "authService=" + authService +
                ", userService=" + userService +
                '}';
    }
}
