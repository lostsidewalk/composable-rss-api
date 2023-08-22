package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.ApiKeyException;
import com.lostsidewalk.buffy.app.user.ApiUserService;
import com.lostsidewalk.buffy.auth.ApiKey;
import com.lostsidewalk.buffy.auth.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
class ApiAuthHandler {

    @Autowired
    AuthService authService;

    @Autowired
    ApiUserService userService;

    void processApiRequest(String apiKey, String apiSecret, String requestUrl, String requestMethod, @SuppressWarnings("unused") HttpServletResponse response)
            throws DataAccessException, ApiKeyException {
        if (isAnyBlank(apiKey, apiSecret)) {
            throw new ApiKeyException("Unable to locate API key or API secret headers");
        }
        User user = authService.findUserByApiKey(apiKey);
        if (user == null) {
            throw new ApiKeyException("Unable to locate API key by UUID");
        }
        ApiKey apiKeyObj = authService.requireApiKey(user.getUsername());
        validateApiKey(apiKeyObj, apiKey, apiSecret);
        String username = user.getUsername();
        UserDetails userDetails = userService.loadUserByUsername(username);
        WebAuthenticationToken authToken = new WebAuthenticationToken(userDetails, apiKey, userDetails.getAuthorities());
        authToken.setDetails(userDetails);
        //
        // !! ACHTUNG !! POINT OF NO RETURN !!
        //
        getContext().setAuthentication(authToken);
        //
        // !! YOU'VE DONE IT NOW !!
        //
        log.debug("Logged in username={} via API header auth for requestUrl={}, requestMethod={}", username, requestUrl, requestMethod);
    }

    private static void validateApiKey(ApiKey apiKeyObj, String apiKey, String apiSecret) throws ApiKeyException {
        // ensure that the key UUIDs match
        if (!StringUtils.equals(apiKey, apiKeyObj.getApiKey())) {
            throw new ApiKeyException("API key mismatch");
        }
        // ensure that the secret values match
        if (!StringUtils.equals(apiSecret, apiKeyObj.getApiSecret())) {
            throw new ApiKeyException("API secret mismatch");
        }
    }
}
