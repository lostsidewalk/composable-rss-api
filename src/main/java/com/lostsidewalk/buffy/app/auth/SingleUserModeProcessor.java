package com.lostsidewalk.buffy.app.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
class SingleUserModeProcessor {

    @Autowired
    LocalUserService localUserService;

    @Autowired
    ApiUserService apiUserService;

    @Value("${comprss.adminUsername:me}")
    String adminUsername;

    void setupLocalSession() {
        UserDetails userDetails = localUserService.loadUserByUsername(adminUsername);
        setupSession(userDetails);
    }

    void setupApiSession() {
        UserDetails userDetails = apiUserService.loadUserByUsername(adminUsername);
        setupSession(userDetails);
    }

    private void setupSession(UserDetails userDetails) {
        WebAuthenticationToken authToken = new WebAuthenticationToken(userDetails, randomAlphanumeric(32), userDetails.getAuthorities());
        authToken.setDetails(userDetails);
        //
        // !! ACHTUNG !! POINT OF NO RETURN !!
        //
        getContext().setAuthentication(authToken);
        //
        // !! YOU'VE DONE IT NOW !!
        //
        log.debug("Setup single-user mode session for adminUsername={}", adminUsername);
    }
}
